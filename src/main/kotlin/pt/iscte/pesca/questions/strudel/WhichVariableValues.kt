package pt.iscte.pesca.questions

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.getVariableAssignments
import pt.iscte.pesca.extensions.sample
import pt.iscte.pesca.extensions.sampleSequentially
import pt.iscte.pesca.questions.Option
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.subtypes.StrudelQuestionRandomProcedure
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.dsl.False
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.collections.set

class WhichVariableValues : StrudelQuestionRandomProcedure() {

    val valuesPerVariable = mutableMapOf<IVariableDeclaration<*>, List<IValue>>()

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

    override fun setup(vm: IVirtualMachine) {
        valuesPerVariable.clear()
        vm.addListener(object : IVirtualMachine.IListener {
            override fun variableAssignment(a: IVariableAssignment, value: IValue) {
                valuesPerVariable[a.target] = (valuesPerVariable[a.target] ?: emptyList()) + listOf(value)
            }
        })
    }

    override fun build(
        source: SourceCode,
        vm: IVirtualMachine,
        procedure: IProcedure,
        arguments: List<IValue>,
        call: String,
        language: Language
    ): QuestionData {
        vm.execute(procedure, *arguments.toTypedArray())

        valuesPerVariable.keys.forEach {
            if (it in procedure.parameters)
                valuesPerVariable[it] =
                    listOf(arguments[procedure.parameters.indexOf(it)]) + (valuesPerVariable[it] ?: emptyList())
        }
        val variable = valuesPerVariable.keys.random()
        val values = valuesPerVariable[variable]!!

        val distractors = sampleSequentially(3,
            if (values.size > 1) listOf(
                values.subList(1, values.size),
                values.subList(0, values.size - 1)
            ) else emptyList(),
            valuesPerVariable.values.filter { it.size > 1 }.map { it.subList(1, it.size) },
            valuesPerVariable.values.filter { it.size > 1 }.map { it.subList(0, it.size - 1) },
            valuesPerVariable.values,
            valuesPerVariable.values.filter { it.size > 1 }.map { it.reversed() },
            valuesPerVariable.values.filter { it.size > 1 }.map { it.reversed().subList(1, it.size) },
            valuesPerVariable.values.filter { it.size > 1 }.map { it.reversed().subList(0, it.size - 1) },
            listOf(listOf(valuesPerVariable.keys.size), arguments)
        ) {
            it != values && it.isNotEmpty()
        }

        val options: MutableMap<Option, Boolean> = mutableMapOf(SimpleTextOption(values) to true)
        distractors.forEach { options[SimpleTextOption(it)] = false }
        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return QuestionData(
            source,
            TextWithCodeStatement(language["WhichVariableValues"].format(variable.id, call), procedure),
            options,
            language = language
        )
    }
}