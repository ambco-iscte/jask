package pt.iscte.pesca.questions.dynamic

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.correctAndRandomDistractors
import pt.iscte.pesca.extensions.getProcedureCalls
import pt.iscte.pesca.extensions.getUsedProceduresWithinModule
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.subtypes.StrudelQuestionRandomProcedure
import pt.iscte.strudel.model.ILoop
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.math.max

class HowManyLoopIterations : StrudelQuestionRandomProcedure() {

    var count: Int = 0

    override fun isApplicable(element: IProcedure): Boolean =
        element.findAll(ILoop::class).size == 1

    override fun setup(vm: IVirtualMachine) {
        count = 0
        vm.addListener(object : IVirtualMachine.IListener {
            override fun loopIteration(loop: ILoop) {
                count++
            }
        })
    }

    override fun build(
        vm: IVirtualMachine,
        procedure: IProcedure,
        arguments: List<IValue>,
        call: String,
        language: Language
    ): QuestionData {
        vm.execute(procedure, *arguments.toTypedArray())
        return QuestionData(
            TextWithCodeStatement(
                language["HowManyLoopIterations"].format(call),
                procedure
            ),
            correctAndRandomDistractors(count, setOf(0, count-1, count+1)),
            language
        )
    }
}