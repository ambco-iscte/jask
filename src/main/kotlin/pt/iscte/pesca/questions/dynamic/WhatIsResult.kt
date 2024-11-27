package pt.iscte.pesca.questions.dynamic

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.sampleSequentially
import pt.iscte.pesca.questions.Option
import pt.iscte.pesca.questions.subtypes.StrudelQuestionRandomProcedure
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.strudel.model.ILiteral
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IReturn
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.text.format

class WhatIsResult: StrudelQuestionRandomProcedure() {

    val valuesPerVariable = mutableMapOf<IVariableDeclaration<*>, List<IValue>>()

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
        val result = vm.execute(procedure, *arguments.toTypedArray())!!

        val returnLiterals = procedure.findAll(IReturn::class).filter {
            it.expression != null && it.expression!!.type is ILiteral
        }.map {
            vm.getValue((it.expression!!.type as ILiteral).stringValue)
        }

        val lastVariableValues = valuesPerVariable.values.map { it.last() }

        val alternativeResults = alternatives.map { vm.execute(procedure, *it.toTypedArray())!! }
        val alternativeArgValues = alternatives.toTypedArray()

        val distractors = sampleSequentially(3, returnLiterals, lastVariableValues, arguments, alternativeResults, *alternativeArgValues) {
            it.value != result.value
        }
        
        val options: MutableMap<Option, Boolean> = distractors.associate {
            SimpleTextOption(it) to false
        }.toMutableMap()
        options[SimpleTextOption(result)] = true

        return QuestionData(
            TextWithCodeStatement(language["WhatIsResult"].format(call), procedure),
            options,
            language = language
        )
    }

    // Return type is a value.
    override fun isApplicable(element: IProcedure): Boolean =
        element.returnType.isValueType
}