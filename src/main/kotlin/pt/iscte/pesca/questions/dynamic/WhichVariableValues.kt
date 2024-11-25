package pt.iscte.pesca.questions.dynamic

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
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.collections.set

class WhichVariableValues : StrudelQuestionRandomProcedure() {

    val valuesPerVariable = mutableMapOf<IVariableDeclaration<*>, List<IValue>>()

    override fun isApplicable(element: IProcedure): Boolean =
        element.getVariableAssignments().any { it.value.size > 1 }

    override fun setup(vm: IVirtualMachine) {
        valuesPerVariable.clear()
        vm.addListener(object : IVirtualMachine.IListener {
            override fun variableAssignment(a: IVariableAssignment, value: IValue) {
                valuesPerVariable[a.target] = (valuesPerVariable[a.target] ?: emptyList()) + listOf(value)
            }
        })
    }

    override fun build(
        vm: IVirtualMachine,
        procedure: IProcedure,
        arguments: List<IValue>,
        alternatives: List<List<IValue>>,
        call: String,
        language: Language
    ): QuestionData {
        vm.execute(procedure, *arguments.toTypedArray())

        val variable = valuesPerVariable.keys.random()
        val values = valuesPerVariable[variable]!!

        val distractors = sampleSequentially(2,
            valuesPerVariable.values.filter { it != values },
            listOf(arguments)
        )

        val options: MutableMap<Option, Boolean> = mutableMapOf(
            SimpleTextOption(values) to true,
            SimpleTextOption(values.reversed()) to false,
            SimpleTextOption(distractors.first()) to false,
            SimpleTextOption(distractors.last()) to false,
        )
        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return QuestionData(
            TextWithCodeStatement(language["WhichVariableValues"].format(variable.id, call), procedure),
            options,
            language = language
        )
    }
}