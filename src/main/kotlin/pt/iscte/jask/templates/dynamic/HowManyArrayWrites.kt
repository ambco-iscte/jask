package pt.iscte.jask.templates.dynamic
import jdk.jfr.Description
import pt.iscte.jask.templates.*

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.countArrayAccesses
import pt.iscte.jask.extensions.getUsedProceduresWithinModule
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.extensions.toIValues
import pt.iscte.jask.extensions.toSetBy
import pt.iscte.strudel.model.IArrayLength
import pt.iscte.strudel.model.IBlock
import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.collections.firstOrNull
import kotlin.collections.joinToString
import kotlin.collections.plusAssign
import kotlin.inc

class HowManyArrayWrites : DynamicQuestionTemplate<IProcedure>() {

    private class HowManyArrayWritesListener(val vm: IVirtualMachine): IVirtualMachine.IListener {
        var countWrites = 0
            private set

        var countReads = 0
            private set

        var len = 0
            private set

        var allocated  = 0
            private set

        val allocations = mutableListOf<Pair<IVariableDeclaration<*>, IArray>>()
        val writes = mutableListOf<String>()

        @Suppress("UNCHECKED_CAST")
        override fun variableAssignment(a: IVariableAssignment, value: IValue) {
            if (value.type.isArrayReference)
                allocations.add(a.target to (value as IReference<IArray>).target)
            else if (value.type.isArray)
                allocations.add(a.target to value as IArray)
        }

        override fun arrayAllocated(ref: IReference<IArray>) {
            allocated++
            len += ref.target.length
            ref.target.addListener(object : IArray.IListener {
                override fun elementChanged(index: Int, oldValue: IValue, newValue: IValue) {
                    if (!vm.callStack.isEmpty) {
                        countWrites++

                        val arrayVar = allocations.firstOrNull { it.second == ref.target }?.first
                        if (arrayVar != null)
                            writes.add("${arrayVar.id}[$index] = $newValue")
                    }
                }

                override fun elementRead(index: Int, value: IValue) {
                    if (!vm.callStack.isEmpty)
                        countReads++
                }
            })
        }
    }

    @Description("Procedure must contain at least 1 array access")
    override fun isApplicable(element: IProcedure): Boolean =
        element.countArrayAccesses() > 0

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        val listener = HowManyArrayWritesListener(vm)
        vm.addListener(listener)

        val arguments = args.toIValues(vm, module)
        vm.execute(procedure, *arguments.toTypedArray())

        var arrayLengthAccess = 0
        procedure.accept(object : IBlock.IVisitor {
            override fun visitAny(exp: IExpression) {
                if (exp is IArrayLength)
                    arrayLengthAccess++
            }
        })

        val distractors: Set<Pair<Int, String?>> = sampleSequentially(3,
            listOf(
                listener.countReads to language["HowManyArrayWrites_DistractorReads"].format("a[i] = x", "x", "a", "i"),
                listener.countReads + 1 to null,
                listener.countReads - 1 to null,
                listener.countWrites + 1 to null,
                listener.countWrites - 1 to null,
                // countReads + countWrites
                arrayLengthAccess to language["HowManyArrayWrites_DistractorLengthAccesses"].format("length")
            ),
            listOf(
                listener.allocated to if (listener.allocated == 0) null else language["HowManyArrayWrites_DistractorNumAllocated"].format(listener.allocations.joinToString { it.first.id!! }),
                listener.allocated + 1 to null,
                listener.allocated - 1 to null,
            ),
            listOf(
                listener.len to if (listener.len == 0) null else language["HowManyArrayWrites_DistractorLengthOfAllocated"].format(),
                listener.len + 1 to null,
                listener.len - 1 to null
            )
        ) {
            it.first != listener.countWrites && it.first >= 0
        }.toSetBy { it.first }

        val options: MutableMap<Option, Boolean> = distractors.associate {
            SimpleTextOption(it.first, it.second) to false
        }.toMutableMap()
        options[SimpleTextOption(
            listener.countWrites,
            language["HowManyArrayWrites_Correct"].format("x", "a", "i", "a[i] = x", listener.writes.joinToString())
        )] = true
        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        val statement = language["HowManyArrayWrites"].orAnonymous(arguments, procedure)
        return Question(
            source,
            TextWithCodeStatement(
                statement.format(procedureCallAsString(procedure, args)),
                listOf(procedure) + procedure.getUsedProceduresWithinModule()
            ),
            options,
            language = language
        )
    }
}