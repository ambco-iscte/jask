package pt.iscte.jask.templates.dynamic
import jdk.jfr.Description
import pt.iscte.jask.templates.*

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.sample
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.extensions.toIValues
import pt.iscte.jask.extensions.toSetBy
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.QuestionOption
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.ILoop
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.IVariableExpression
import pt.iscte.strudel.model.util.find
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

class HowManyLoopIterations : DynamicQuestionTemplate<IProcedure>() {

    var count: Int = 0
    val variableHistories = mutableMapOf<IVariableDeclaration<*>, List<IValue>>()

    @Description("Procedure must contain exactly one loop")
    override fun isApplicable(element: IProcedure): Boolean =
        element.findAll(ILoop::class).size == 1

    fun setup(vm: IVirtualMachine) {
        count = 0
        variableHistories.clear()
        vm.addListener(object : IVirtualMachine.IListener {
            override fun loopIteration(loop: ILoop) {
                count++
            }

            override fun variableAssignment(a: IVariableAssignment, value: IValue) {
                variableHistories[a.target] = (variableHistories[a.target] ?: emptyList()).plus(value)
            }
        })
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        setup(vm)
        val arguments = args.toIValues(vm, module)

        val guard = procedure.find(ILoop::class).guard
        val variablesInGuard = mutableSetOf<IVariableDeclaration<*>>()
        guard.accept(object : IExpression.IVisitor {
            override fun visit(exp: IVariableExpression) {
                variablesInGuard.add(exp.variable)
            }
        })

        vm.execute(procedure, *arguments.toTypedArray())

        val distractors = sampleSequentially(3, listOf(
            count + 1 to language["HowManyLoopIterations_DistractorOneMore"].format(guard.toString(), "false"),
            count + 2 to null,
            count + 3 to null,
            count - 1 to null,
            count - 2 to null,
            count - 3 to null,
        )) {
            it.first != count && it.first >= 0
        }.toSetBy { it.first }

        val options: MutableMap<QuestionOption, Boolean> = distractors.associate {
            SimpleTextOption(it.first, it.second) to false
        }.toMutableMap()
        options[SimpleTextOption(count)] = true

        if (options.size < 4)
            options[SimpleTextOption(guard.toString())] = false

        if (options.size < 4)
            variablesInGuard.sample(4 - options.size).forEach {
                options[SimpleTextOption(it.id)] = false
            }

        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        val statement = language["HowManyLoopIterations"].orAnonymous(arguments, procedure)
        return Question(
            source,
            TextWithCodeStatement(
                statement.format(procedureCallAsString(procedure, args)),
                procedure
            ),
            options,
            language
        )
    }
}