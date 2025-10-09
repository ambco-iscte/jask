package pt.iscte.jask.templates.dynamic
import pt.iscte.jask.templates.*

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.correctAndRandomDistractors
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.toIValues
import pt.iscte.strudel.model.IArrayAllocation
import pt.iscte.strudel.model.IBlock
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

class HowManyArrayAllocations : DynamicQuestionTemplate<IProcedure>() {

    private data class HowManyArrayAllocationsListener(val vm: IVirtualMachine): IVirtualMachine.IListener {
        val allocations = mutableListOf<Int>()
        val allocated = mutableListOf<Pair<IVariableDeclaration<*>, IArray>>()

        override fun arrayAllocated(ref: IReference<IArray>) {
            // exclude args allocation
            if (!vm.callStack.isEmpty)
                allocations.add(ref.target.length)
        }

        @Suppress("UNCHECKED_CAST")
        override fun variableAssignment(a: IVariableAssignment, value: IValue) {
            if (value.type.isArrayReference)
                allocated.add(a.target to (value as IReference<IArray>).target)
            else if (value.type.isArray)
                allocated.add(a.target to value as IArray)
        }
    }

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

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        val listener = HowManyArrayAllocationsListener(vm)
        vm.addListener(listener)

        val arguments = args.toIValues(vm, module)
        vm.execute(procedure, *arguments.toTypedArray())

        var countAllocationInstructions = 0 // min: 2
        val v = object : IBlock.IVisitor {
            override fun visit(exp: IArrayAllocation): Boolean {
                countAllocationInstructions++
                return true
            }
        }
        procedure.block.accept(v)

        val distractors = mutableSetOf<Pair<Any, String?>>(
            countAllocationInstructions + procedure.parameters.count { it.type.isArrayReference } to null,
            procedure.parameters.count {
                it.type.isArrayReference
            } to language["HowManyArrayAllocations_DistractorArrayParams"].format(procedure.id),
            listener.allocations.sum() to language["HowManyArrayAllocations_DistractorTotalLength"].format(),
            countAllocationInstructions to null,
            listener.allocations.size + 1 to null,
            if (listener.allocations.isNotEmpty())
                listener.allocations.size - 1 to null
            else
                0 to null,
            0 to null
        )

        if (distractors.size < 3)
            distractors.add(language["NoneOfTheAbove"] to null)

        val statement = language[HowManyArrayAllocations::class.simpleName!!].orAnonymous(arguments, procedure)
        return Question(
            source,
            TextWithCodeStatement(
                statement.format(procedureCallAsString(procedure, args)),
                procedure
            ),
            correctAndRandomDistractors(
                listener.allocations.size to language["HowManyArrayAllocations_Correct"].format(
                    listener.allocated.joinToString { it.first.id!! }
                ),
                distractors.toMap()
            ),
            language = language
        )
    }
}