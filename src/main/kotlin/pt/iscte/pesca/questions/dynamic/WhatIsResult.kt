package pt.iscte.pesca.questions.dynamic

import com.github.javaparser.ast.stmt.ReturnStmt
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.extensions.sample
import pt.iscte.pesca.questions.Option
import pt.iscte.pesca.questions.subtypes.StrudelQuestionRandomProcedure
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.strudel.model.DOUBLE
import pt.iscte.strudel.model.ILiteral
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IReturn
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.model.util.isIntLiteral
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.text.format

class WhatIsResult: StrudelQuestionRandomProcedure() {

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

        val distractors = returnLiterals.filter {
            it.value != result.value
        }.toSet().sample(3 - returnLiterals.size)
        
        val options: MutableMap<Option, Boolean> = (result.multipleChoice(language) + distractors.associate {
            SimpleTextOption(it) to false
        }).toMutableMap()
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