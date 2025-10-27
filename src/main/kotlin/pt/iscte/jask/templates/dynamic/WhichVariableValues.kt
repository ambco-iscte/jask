package pt.iscte.jask.templates.dynamic
import pt.iscte.jask.templates.*

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getVariableAssignments
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.extensions.toIValues
import pt.iscte.jask.extensions.toSetBy
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.collections.set

class WhichVariableValues : DynamicQuestionTemplate<IProcedure>() {

    var procedure: IProcedure? = null
    val valuesPerVariable = mutableMapOf<IVariableDeclaration<*>, List<IValue>>()

    companion object {
        fun options(
            correct: IVariableDeclaration<*>,
            values: List<IValue>,
            variableHistory: Map<IVariableDeclaration<*>, List<IValue>>,
            arguments: List<Any?>,
            language: Language
        ): Map<Option, Boolean> {
            require(values.size > 1) { "WhichVariableValues variable must take at least 2 values!" }

            val otherValues = variableHistory.values.filter { it.size > 1 }
            val distractors = sampleSequentially(3,
                if (values.size > 1) listOf(
                    values.subList(1, values.size) to null,                     // Correct with first removed
                    values.subList(0, values.size - 1) to null                  // Correct with last removed
                ) else emptyList(),
                otherValues.map { it.subList(1, it.size) to null },             // Others with first removed
                otherValues.map { it.subList(0, it.size - 1) to null },         // Others with last removed

                // Other variables
                variableHistory.map {
                    it.value to
                    language["WhichVariableValues_DistractorWrongVariable"].format(it.key.id, correct.id)
                },

                otherValues.flatMap {
                    val reversed = it.reversed()
                    listOf(
                        reversed to null,                                       // Others reversed
                        reversed.subList(1, it.size) to null,                   // Others reversed with first removed
                        reversed.subList(0, it.size - 1) to null                // Others reversed with last removed
                    )
                },
                listOf(listOf(variableHistory.keys.size) to null, arguments to null)
            ) {
                it.first != values && it.first.isNotEmpty()
            }.toSetBy { it.first }

            val options: MutableMap<Option, Boolean> = mutableMapOf()

            distractors.forEach {
                options[SimpleTextOption(it.first, it.second)] = false
            }

            options[SimpleTextOption(values)] = true

            if (options.size < 4)
                options[SimpleTextOption.none(language)] = false

            return options
        }
    }

    // At least one variable was assigned at least 2 values.
    override fun isApplicable(element: IProcedure): Boolean =
        element.getVariableAssignments().any { it.value.size > 1 }

    override fun isApplicable(element: IProcedure, args: List<IValue>): Boolean {
        val vm = IVirtualMachine.create()
        val variableHistory = mutableMapOf<IVariableDeclaration<*>, List<IValue>>()
        vm.addListener(object : IVirtualMachine.IListener {
            override fun variableAssignment(a: IVariableAssignment, value: IValue) {
                if (a.ownerProcedure == element)
                    variableHistory[a.target] = (variableHistory[a.target] ?: emptyList()).plus(value)
            }
        })
        vm.execute(element, *args.toTypedArray())
        return variableHistory.values.any { it.size > 1 }
    }

    fun setup(vm: IVirtualMachine) {
        valuesPerVariable.clear()
        vm.addListener(object : IVirtualMachine.IListener {
            override fun variableAssignment(a: IVariableAssignment, value: IValue) {
                if (a.ownerProcedure == procedure)
                    valuesPerVariable[a.target] = (valuesPerVariable[a.target] ?: emptyList()) + listOf(value)
            }
        })
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, module, procedure, args) = getRandomProcedure(sources)
        this.procedure = procedure

        val vm = IVirtualMachine.create()
        setup(vm)
        val arguments = args.toIValues(vm, module)

        vm.execute(procedure, *arguments.toTypedArray())

        valuesPerVariable.keys.forEach {
            if (it in procedure.parameters) {
                val argument = arguments[procedure.parameters.indexOf(it)]
                valuesPerVariable[it] = listOf(argument) + (valuesPerVariable[it] ?: emptyList())
            }
        }
        val variable = valuesPerVariable.filter { it.value.size > 1 }.keys.random()
        val values = valuesPerVariable[variable]!!

        val statement = language["WhichVariableValues"].orAnonymous(arguments, procedure)
        return Question(
            source,
            TextWithCodeStatement(
                statement.format(variable.id, procedureCallAsString(procedure, args)),
                procedure
            ),
            options(variable, values, valuesPerVariable, args, language),
            language = language,
            relevantSourceCode = procedure.findAll(IVariableAssignment::class).filter {
                it.target == variable
            }.map { SourceLocation(it) }
        )
    }
}