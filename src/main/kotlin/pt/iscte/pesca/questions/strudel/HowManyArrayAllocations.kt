package pt.iscte.pesca.questions

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.correctAndRandomDistractors
import pt.iscte.pesca.questions.subtypes.StrudelQuestionRandomProcedure
import pt.iscte.strudel.model.IArrayAllocation
import pt.iscte.strudel.model.IBlock
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

class HowManyArrayAllocations : StrudelQuestionRandomProcedure() {
    val allocations = mutableListOf<Int>()

    override fun isApplicable(element: IProcedure): Boolean {
        var count = 0
        val v = object : IBlock.IVisitor {
            override fun visit(exp: IArrayAllocation): Boolean {
                count++
                return true
            }
        }
        element.block.accept(v)
        return count > 0
    }


    override fun setup(vm: IVirtualMachine) {
        vm.addListener(object : IVirtualMachine.IListener {
            override fun arrayAllocated(ref: IReference<IArray>) {
                // exclude args allocation
                if(!vm.callStack.isEmpty)
                    allocations.add(ref.target.length)
            }
        })
    }

    override fun build(
        source: SourceCode,
        vm: IVirtualMachine,
        procedure: IProcedure,
        arguments: List<IValue>,
        call: String,
        language: Language
    ): QuestionData {
        vm.execute(procedure, *arguments.toTypedArray())

        var countAllocationInstructions = 0 // min: 2
        val v = object : IBlock.IVisitor {
            override fun visit(exp: IArrayAllocation): Boolean {
                countAllocationInstructions++
                return true;
            }
        }
        procedure.block.accept(v)

        val distractors = mutableSetOf<Any>(
            countAllocationInstructions + procedure.parameters.count { it.type.isArrayReference },
            procedure.parameters.count { it.type.isArrayReference },
            countAllocationInstructions,
            allocations.size + 1,
            if (allocations.size > 0) allocations.size - 1 else 0,
            0
        )

        if(distractors.size < 3)
            distractors.add(language["NoneOfTheAbove"])

        return QuestionData(
            source,
            TextWithCodeStatement(
                language[HowManyArrayAllocations::class.simpleName!!].format(
                    call
                ), procedure
            ),
            correctAndRandomDistractors(allocations.size, distractors),
            language = language
        )
    }
}