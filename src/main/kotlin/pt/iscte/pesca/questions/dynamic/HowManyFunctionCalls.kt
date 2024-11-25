package pt.iscte.pesca.questions.dynamic

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.correctAndRandomDistractors
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
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

class HowManyFunctionCalls : StrudelQuestionRandomProcedure() {
    var count: MutableMap<String, Int> = mutableMapOf()

    override fun isApplicable(element: IProcedure): Boolean =
        element.getProcedureCalls().isNotEmpty()

    override fun setup(vm: IVirtualMachine) {
        vm.addListener(object : IVirtualMachine.IListener {
            override fun procedureCall(procedure: IProcedureDeclaration, args: List<IValue>, caller: IProcedure?) {
                val p = procedure.id!!
                if(!vm.callStack.isEmpty) {
                    if(count.putIfAbsent(p, 1) == null)
                        count[p] = count[p]!! + 1
                }
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

        val depProcedures = procedure.getProcedureCalls().map { it.procedure }.toSet()

        val randomProcedure = depProcedures.random()

        val correct = count[randomProcedure.id!!] ?: 0

        return QuestionData(
            TextWithCodeStatement(
                language["HowManyFunctionCalls"].format(randomProcedure.id, call),
                listOf(procedure) + procedure.getUsedProceduresWithinModule()
            ),
            correctAndRandomDistractors(correct,
                setOf(
                    procedure.getProcedureCalls().size,
                    depProcedures.size,
                    correct + 1,
                    correct - 1
                )
            ),
            language
        )
    }
}