package pt.iscte.jask.templates.dynamic
import pt.iscte.jask.templates.*

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getVariableAssignments
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.extensions.toIValues
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.collections.set

class WhichVariableValues : DynamicQuestionTemplate<IProcedure>() {

    val valuesPerVariable = mutableMapOf<IVariableDeclaration<*>, List<IValue>>()

    companion object {
        fun options(
            values: List<IValue>,
            variableHistory: Map<IVariableDeclaration<*>, List<IValue>>,
            arguments: List<IValue>,
            language: Language
        ): Map<Option, Boolean> {
            val distractors = sampleSequentially(3,
                if (values.size > 1) listOf(
                    values.subList(1, values.size),
                    values.subList(0, values.size - 1)
                ) else emptyList(),
                variableHistory.values.filter { it.size > 1 }.map { it.subList(1, it.size) },
                variableHistory.values.filter { it.size > 1 }.map { it.subList(0, it.size - 1) },
                variableHistory.values,
                variableHistory.values.filter { it.size > 1 }.map { it.reversed() },
                variableHistory.values.filter { it.size > 1 }.map { it.reversed().subList(1, it.size) },
                variableHistory.values.filter { it.size > 1 }.map { it.reversed().subList(0, it.size - 1) },
                listOf(listOf(variableHistory.keys.size), arguments)
            ) {
                it != values && it.isNotEmpty()
            }

            val options: MutableMap<Option, Boolean> = mutableMapOf(SimpleTextOption(values) to true)
            distractors.forEach { options[SimpleTextOption(it)] = false }
            if (options.size < 4)
                options[SimpleTextOption.none(language)] = false

            return options
        }
    }

    override fun isApplicable(element: IProcedure): Boolean =
        element.getVariableAssignments().any { it.value.size > 1 }

    override fun isApplicable(element: IProcedure, args: List<IValue>): Boolean {
        val vm = IVirtualMachine.create()
        val assigns = mutableMapOf<IVariableDeclaration<*>, List<IValue>>()
        vm.addListener(object : IVirtualMachine.IListener {
            override fun variableAssignment(a: IVariableAssignment, value: IValue) {
                assigns[a.target] = (assigns[a.target] ?: emptyList()) + listOf(value)
            }
        })
        vm.execute(element, *args.toTypedArray())
        return assigns.keys.size > 1 && assigns.values.any { it.size > 1 }
    }

    fun setup(vm: IVirtualMachine) {
        valuesPerVariable.clear()
        vm.addListener(object : IVirtualMachine.IListener {
            override fun variableAssignment(a: IVariableAssignment, value: IValue) {
                valuesPerVariable[a.target] = (valuesPerVariable[a.target] ?: emptyList()) + listOf(value)
            }
        })
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        setup(vm)
        val arguments = args.toIValues(vm, module)

        vm.execute(procedure, *arguments.toTypedArray())

        valuesPerVariable.keys.forEach {
            if (it in procedure.parameters)
                valuesPerVariable[it] =
                    listOf(arguments[procedure.parameters.indexOf(it)]) + (valuesPerVariable[it] ?: emptyList())
        }
        val variable = valuesPerVariable.keys.random()
        val values = valuesPerVariable[variable]!!

        val statement = language["WhichVariableValues"].orAnonymous(arguments, procedure)
        return Question(
            source,
            TextWithCodeStatement(
                statement.format(variable.id, procedureCallAsString(procedure, arguments)),
                procedure
            ),
            options(values, valuesPerVariable, arguments, language),
            language = language,
            relevantSourceCode = procedure.findAll(IVariableAssignment::class).filter { it.target == variable }.map { SourceLocation(it) }
        )
    }
}