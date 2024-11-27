package pt.iscte.pesca.questions.dynamic

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.correctAndRandomDistractors
import pt.iscte.pesca.extensions.getProcedureCalls
import pt.iscte.pesca.extensions.getUsedProceduresWithinModule
import pt.iscte.pesca.extensions.sample
import pt.iscte.pesca.extensions.sampleSequentially
import pt.iscte.pesca.questions.Option
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.subtypes.StrudelQuestionRandomProcedure
import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.ILoop
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.model.IVariableExpression
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.math.max

class HowManyLoopIterations : StrudelQuestionRandomProcedure() {

    var count: Int = 0
    var guard: IExpression? = null

    override fun isApplicable(element: IProcedure): Boolean =
        element.findAll(ILoop::class).size == 1

    override fun setup(vm: IVirtualMachine) {
        count = 0
        guard = null
        vm.addListener(object : IVirtualMachine.IListener {
            override fun loopIteration(loop: ILoop) {
                count++
                if (guard == null)
                    guard = loop.guard
            }
        })
    }

    override fun build(
        vm: IVirtualMachine,
        procedure: IProcedure,
        arguments: List<IValue>,
        call: String,
        language: Language
    ): QuestionData {
        vm.execute(procedure, *arguments.toTypedArray())

        val distractors = sampleSequentially(3,
            listOf(count + 1, count - 1, count + 2),
        ) {
            it != count && it >= 0
        }

        val variablesInGuard = mutableSetOf<String>()
        guard?.accept(object : IExpression.IVisitor {
            override fun visit(exp: IVariableExpression) {
                if (exp.variable.id != null)
                    variablesInGuard.add(exp.variable.id!!)
            }
        })

        val options: MutableMap<Option, Boolean> = distractors.associate {
            SimpleTextOption(it) to false
        }.toMutableMap()
        options[SimpleTextOption(count)] = true
        if (options.size < 4 && guard != null)
            options[SimpleTextOption(guard)] = false
        if (options.size < 4)
            variablesInGuard.sample(4 - options.size).forEach {
                options[SimpleTextOption(it)] = false
            }
        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return QuestionData(
            TextWithCodeStatement(
                language["HowManyLoopIterations"].format(call),
                procedure
            ),
            options,
            language
        )
    }
}