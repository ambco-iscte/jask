package pt.iscte.pesca.questions.dynamic

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.getUsedProceduresWithinModule
import pt.iscte.pesca.extensions.isSelfContained
import pt.iscte.pesca.extensions.sampleSequentially
import pt.iscte.pesca.questions.Option
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

class HowManyArrayReads : StrudelQuestionRandomProcedure() {
    var count = 0
    var len = 0
    var allocated = 0

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
        allocated = 0
        vm.addListener(object : IVirtualMachine.IListener {
            override fun arrayAllocated(ref: IReference<IArray>) {
                allocated++
                len += ref.target.length
                ref.target.addListener(object : IArray.IListener {
                    override fun elementRead(index: Int, value: IValue) {
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

        val distractors = sampleSequentially(3,
            listOf(allocated, count + 1, count - 1, allocated + 1, allocated - 1),
            listOf(len, len + 1, len - 1)
        ) {
            it != count && it >= 0
        }

        val options: MutableMap<Option, Boolean> = distractors.associate {
            SimpleTextOption(it) to false
        }.toMutableMap()
        options[SimpleTextOption(count)] = true
        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return QuestionData(
            TextWithCodeStatement(
                language["HowManyArrayReads"].format(call),
                listOf(procedure) + procedure.getUsedProceduresWithinModule()
            ),
            options,
            language = language
        )
    }
}