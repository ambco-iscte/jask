package pt.iscte.pesca.questions

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.correctAndRandomDistractors
import pt.iscte.pesca.extensions.procedureCallAsString
import pt.iscte.pesca.extensions.toIValues
import pt.iscte.strudel.model.IArrayAllocation
import pt.iscte.strudel.model.IBlock
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IVirtualMachine

class HowManyArrayAllocations : DynamicQuestion<IProcedure>() {
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

    fun setup(vm: IVirtualMachine) {
        vm.addListener(object : IVirtualMachine.IListener {
            override fun arrayAllocated(ref: IReference<IArray>) {
                // exclude args allocation
                if(!vm.callStack.isEmpty)
                    allocations.add(ref.target.length)
            }
        })
    }

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        setup(vm)
        val arguments = args.toIValues(vm, module)

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
            if (allocations.isNotEmpty()) allocations.size - 1 else 0,
            0
        )

        if(distractors.size < 3)
            distractors.add(language["NoneOfTheAbove"])

        return QuestionData(
            source,
            TextWithCodeStatement(
                language[HowManyArrayAllocations::class.simpleName!!].format(procedureCallAsString(procedure, arguments)), procedure
            ),
            correctAndRandomDistractors(allocations.size, distractors),
            language = language
        )
    }
}