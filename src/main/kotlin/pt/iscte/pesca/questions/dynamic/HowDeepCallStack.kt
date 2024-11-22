package pt.iscte.pesca.questions.dynamic

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.getProcedureCalls
import pt.iscte.pesca.extensions.getUsedProceduresWithinModule
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.subtypes.StrudelQuestionRandomProcedure
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.math.max

class HowDeepCallStack : StrudelQuestionRandomProcedure() {

    var depth: Int = 0
    var numFunctionCalls: Int = 0

    override fun isApplicable(element: IProcedure): Boolean =
        element.getProcedureCalls().isNotEmpty()

    override fun setup(vm: IVirtualMachine) {
        depth = 0
        numFunctionCalls = 0
        vm.addListener(object : IVirtualMachine.IListener {
            override fun procedureCall(procedure: IProcedureDeclaration, args: List<IValue>, caller: IProcedure?) {
                numFunctionCalls++
                depth = max(depth, vm.callStack.size)
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
        return QuestionData(
            TextWithCodeStatement(
                language["HowDeepCallStack"].format(call),
                listOf(procedure) + procedure.getUsedProceduresWithinModule()
            ),
            mapOf(
                SimpleTextOption(depth) to true,
                SimpleTextOption(depth+1) to false,
                SimpleTextOption(if(numFunctionCalls != depth) numFunctionCalls else depth-1) to false,
                (if (depth != 0) SimpleTextOption(0) else SimpleTextOption.none(language)) to false,
            ),
            language = language
        )
    }
}