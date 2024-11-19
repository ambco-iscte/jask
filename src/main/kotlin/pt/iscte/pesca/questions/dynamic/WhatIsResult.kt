package pt.iscte.pesca.questions.dynamic

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.questions.subtypes.StrudelQuestionRandomProcedure
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.text.format

class WhatIsResult: StrudelQuestionRandomProcedure() {

    override fun build(
        vm: IVirtualMachine,
        procedure: IProcedure,
        arguments: List<IValue>,
        call: String,
        language: Language
    ): QuestionData {
        val result = vm.execute(procedure, *arguments.toTypedArray())!!
        return QuestionData(
            TextWithCodeStatement(language["WhatIsResult"].format(call), procedure),
            result.multipleChoice(language),
            language = language
        )
    }

    // Return type is a value.
    override fun isApplicable(element: IProcedure): Boolean =
        element.returnType.isValueType
}