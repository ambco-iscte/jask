package pt.iscte.jask.templates.dynamic
import pt.iscte.jask.templates.*

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getVariableAssignments
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.randomKeyByOrNull
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
import kotlin.collections.plus
import kotlin.collections.set
import kotlin.text.get

class WhichVariableValues : DynamicQuestionTemplate<IProcedure>() {

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

    private class WhichVariableValuesListener(
        val vm: IVirtualMachine,
        val procedure: IProcedure,
        val arguments: List<IValue>
    ): IVirtualMachine.IListener {
        private val variableHistory = mutableMapOf<IVariableDeclaration<*>, List<IValue>>()

        fun getVariableHistory(): Map<IVariableDeclaration<*>, List<IValue>> =
            variableHistory.mapValues {
                if (it.key in procedure.parameters) {
                    val argument = arguments[procedure.parameters.indexOf(it.key)]
                    listOf(argument) + it.value
                } else it.value
            }

        override fun variableAssignment(a: IVariableAssignment, value: IValue) {
            if (!vm.callStack.isEmpty && a.ownerProcedure == procedure) {
                variableHistory[a.target] = (variableHistory[a.target] ?: emptyList()).plus(value)
            }
        }
    }

    override fun isApplicable(element: IProcedure): Boolean =
        element.getVariableAssignments().any { it.value.isNotEmpty() }

    override fun isApplicable(element: IProcedure, args: List<IValue>): Boolean {
        val vm = IVirtualMachine.create()
        val listener = WhichVariableValuesListener(vm, element, args)
        vm.addListener(listener)

        vm.execute(element, *args.toTypedArray())

        return listener.getVariableHistory().any { it.value.size > 1 }
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, module, procedure, args) = getRandomProcedure(sources)
        val callAsString = procedureCallAsString(procedure, args)

        val vm = IVirtualMachine.create()
        val arguments = args.toIValues(vm, module)
        val listener = WhichVariableValuesListener(vm, procedure, arguments)
        vm.addListener(listener)

        vm.execute(procedure, *arguments.toTypedArray())

        val variableHistory = listener.getVariableHistory()

        val variable = variableHistory.randomKeyByOrNull { it.value.size > 1 } ?:
        throw ApplicableProcedureCallNotFoundException(
            template = this,
            runtimeErrors = mapOf(source to listOf(
                NoSuchElementException("No variable within $callAsString takes more than 1 value:\n$procedure\n$variableHistory")
            )),
            loadingErrors = emptyMap()
        )

        val values = variableHistory[variable]!!

        val statement = language["WhichVariableValues"].orAnonymous(arguments, procedure)
        return Question(
            source,
            TextWithCodeStatement(
                statement.format(variable.id, callAsString),
                procedure
            ),
            options(variable, values, variableHistory, args, language),
            language = language,
            relevantSourceCode = procedure.findAll(IVariableAssignment::class).filter {
                it.target == variable
            }.map { SourceLocation(it) }
        )
    }
}