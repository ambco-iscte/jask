package pt.iscte.pesca.questions.dynamic

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.getProcedureCalls
import pt.iscte.pesca.extensions.getUsedProceduresWithinModule
import pt.iscte.pesca.extensions.isSelfContained
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.subtypes.StrudelQuestionRandomProcedure
import pt.iscte.strudel.model.IBlock
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IProcedureCall
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

class HowManyFunctionCalls : StrudelQuestionRandomProcedure() {
    var count = 0

    // There is at least one call statement.
    override fun isApplicable(element: IProcedure): Boolean =
        element.getProcedureCalls().isNotEmpty()

    override fun setup(vm: IVirtualMachine) {
        count = 0
        vm.addListener(object : IVirtualMachine.IListener {
            override fun procedureCall(procedure: IProcedureDeclaration, args: List<IValue>, caller: IProcedure?) {
                count++
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

        val num = procedure.getProcedureCalls().size

        return QuestionData(
            TextWithCodeStatement(
                language["HowManyFunctionCalls"].format(call),
                listOf(procedure) + procedure.getUsedProceduresWithinModule()
            ),
            mapOf(
                SimpleTextOption(count) to true,
                SimpleTextOption(count+1) to false,
                SimpleTextOption(if(num != count) num else count-1) to false,
                (if (count != 0) SimpleTextOption(0) else SimpleTextOption.none(language)) to false,
            ),
            language = language
        )
    }
}