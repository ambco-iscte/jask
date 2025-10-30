package pt.iscte.jask.templates.dynamic
import pt.iscte.jask.templates.*

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.correctAndRandomDistractors
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.extensions.toIValues
import pt.iscte.jask.extensions.toSetBy
import pt.iscte.strudel.model.IArrayAccess
import pt.iscte.strudel.model.IArrayAllocation
import pt.iscte.strudel.model.IArrayElementAssignment
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

        var countReads = 0
            private set

        var countWrites = 0
            private set

        override fun arrayAllocated(ref: IReference<IArray>) {
            // exclude args allocation
            if (!vm.callStack.isEmpty)
                allocations.add(ref.target.length)

            ref.target.addListener(object : IArray.IListener {
                override fun elementRead(index: Int, value: IValue) {
                    if (!vm.callStack.isEmpty)
                        countReads++
                }

                override fun elementChanged(index: Int, oldValue: IValue, newValue: IValue) {
                    if (!vm.callStack.isEmpty)
                        countWrites++
                }
            })
        }

        @Suppress("UNCHECKED_CAST")
        override fun variableAssignment(a: IVariableAssignment, value: IValue) {
            if (value.type.isArrayReference)
                allocated.add(a.target to (value as IReference<IArray>).target)
            else if (value.type.isArray)
                allocated.add(a.target to value as IArray)
        }
    }

    // Has any array allocations, OR has any array writes, OR has any array reads.
    override fun isApplicable(element: IProcedure): Boolean {
        var applicable = false
        val v = object : IBlock.IVisitor {
            override fun visit(exp: IArrayAllocation): Boolean {
                applicable = true
                return false
            }

            override fun visit(assignment: IArrayElementAssignment): Boolean {
                applicable = true
                return false
            }

            override fun visit(exp: IArrayAccess): Boolean {
                applicable = true
                return false
            }
        }
        element.block.accept(v)
        return applicable
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

        val distractors: Set<Pair<Int, String?>> = sampleSequentially(3,
            listOf(
                countAllocationInstructions + procedure.parameters.count { it.type.isArrayReference } to null,
                procedure.parameters.count {
                    it.type.isArrayReference
                } to language["HowManyArrayAllocations_DistractorArrayParams"].format(procedure.id),
                procedure.parameters.count {
                    it.type.isArrayReference
                } to language["HowManyArrayAllocations_DistractorArrayParams"].format(procedure.id),
                listener.countWrites to null,
                listener.countReads to null,
            ), listOf(
                countAllocationInstructions + procedure.parameters.count { it.type.isArrayReference } to null,
                listener.allocations.sum() to language["HowManyArrayAllocations_DistractorTotalLength"].format(),
                countAllocationInstructions to null,
                listener.allocations.size + 1 to null,
                if (listener.allocations.isNotEmpty())
                    listener.allocations.size - 1 to null
                else
                    0 to null,
                0 to null
            )
        ) {
            it.first != listener.allocations.size && it.first >= 0
        }.toSetBy { it.first }

        val options: MutableMap<Option, Boolean> = distractors.associate {
            SimpleTextOption(it.first, it.second) to false
        }.toMutableMap()

        options[SimpleTextOption(
            listener.allocations.size,
            if (listener.allocations.isNotEmpty()) language["HowManyArrayAllocations_Correct"].format(
                listener.allocated.joinToString { it.first.id!! }
            ) else null
        )] = true

        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        val statement = language[HowManyArrayAllocations::class.simpleName!!].orAnonymous(arguments, procedure)
        return Question(
            source,
            TextWithCodeStatement(
                statement.format(procedureCallAsString(procedure, args)),
                procedure
            ),
            options,
            language = language
        )
    }
}