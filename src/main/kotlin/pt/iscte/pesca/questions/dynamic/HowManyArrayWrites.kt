package pt.iscte.pesca.questions.dynamic

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.getUsedProceduresWithinModule
import pt.iscte.pesca.extensions.isSelfContained
import pt.iscte.pesca.questions.subtypes.StrudelQuestionRandomProcedure
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.strudel.model.IArrayAccess
import pt.iscte.strudel.model.IBlock
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.text.format

class HowManyArrayWrites : StrudelQuestionRandomProcedure() {

    var count = 0
    var len = 0

    // There is at least one array access.
    override fun isApplicable(element: IProcedure): Boolean {
        var count = 0
        val v = object : IBlock.IVisitor {
            override fun visit(exp: IArrayAccess): Boolean {
                count++
                return true
            }
        }
        element.block.accept(v)
        return count != 0
    }

    override fun setup(vm: IVirtualMachine) {
        count = 0
        len = 0
        vm.addListener(object : IVirtualMachine.IListener {
            override fun arrayAllocated(ref: IReference<IArray>) {
                len += ref.target.length
                ref.target.addListener(object : IArray.IListener {
                    override fun elementChanged(index: Int, oldValue: IValue, newValue: IValue) {
                        count++
                    }
                })
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
                language["HowManyArrayWrites"].format(call),
                listOf(procedure) + procedure.getUsedProceduresWithinModule()
            ),
            mapOf(
                SimpleTextOption(count) to true,
                SimpleTextOption(count+1) to false,
                SimpleTextOption(if(len != count) len else count-1) to false,
                (if (count != 0) SimpleTextOption(0) else SimpleTextOption.none(language)) to false,
            ),
            language = language
        )
    }
}