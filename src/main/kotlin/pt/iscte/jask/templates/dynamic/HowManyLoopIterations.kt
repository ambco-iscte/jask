package pt.iscte.jask.templates.dynamic
import pt.iscte.jask.templates.*

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.sample
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.extensions.toIValues
import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.ILoop
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableExpression
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.vm.IVirtualMachine

class HowManyLoopIterations : DynamicQuestionTemplate<IProcedure>() {

    var count: Int = 0
    var guard: IExpression? = null

    override fun isApplicable(element: IProcedure): Boolean =
        element.findAll(ILoop::class).size == 1

    fun setup(vm: IVirtualMachine) {
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

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        setup(vm)
        val arguments = args.toIValues(vm, module)

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

        val statement = language["HowManyLoopIterations"].orAnonymous(arguments, procedure)
        return Question(
            source,
            TextWithCodeStatement(
                statement.format(procedureCallAsString(procedure, arguments)),
                procedure
            ),
            options,
            language
        )
    }
}