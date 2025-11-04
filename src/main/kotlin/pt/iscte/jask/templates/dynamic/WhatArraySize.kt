package pt.iscte.jask.templates.dynamic
import pt.iscte.jask.templates.*

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.correctAndRandomDistractors
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.extensions.toIValues
import pt.iscte.strudel.model.IArrayAllocation
import pt.iscte.strudel.model.IBlock
import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

inline fun <reified T: IExpression> IBlock.findExpression(): T? {
    var find: T? = null
    accept(object : IBlock.IVisitor {
        override fun visitAny(exp: IExpression) {
            if(exp is T && find == null)
                find = exp
        }
    })
    return find
}

inline fun <reified T: IExpression> IBlock.findAllExpression(): List<T> {
    val find: MutableList<T> = mutableListOf()
    accept(object : IBlock.IVisitor {
        override fun visitAny(exp: IExpression) {
            if(exp is T)
                find.add(exp)
        }
    })
    return find
}

class WhatArraySize : DynamicQuestionTemplate<IProcedure>() {

    private data class WhatArraySizeListener(val vm: IVirtualMachine): IVirtualMachine.IListener {
        val allocations = mutableListOf<Int>()

        var countReads = 0
            private set

        var countWrites = 0
            private set

        override fun arrayAllocated(ref: IReference<IArray>) {
            // exclude args allocation
            if(!vm.callStack.isEmpty)
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
    }

    override fun isApplicable(element: IProcedure): Boolean =
        element.block.findExpression<IArrayAllocation>() != null

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        val listener = WhatArraySizeListener(vm)
        vm.addListener(listener)

        val arguments = args.toIValues(vm, module)
        vm.execute(procedure, *arguments.toTypedArray())

        val arrayArgsLengths = arguments.filter { it is IReference<*> && it.target is IArray }.mapIndexed { i, value ->
            ((value as IReference<*>).target as IArray).length to
            language["WhatArraySize_DistractorParameter"].format("${procedure.parameters[i].id} = $value")
        }

        val distractors: Set<Pair<Any, String?>> = (
            setOf(listener.countReads to null, listener.countWrites to null) +
            listener.allocations.flatMap { listOf(it + 1 to null, it - 1 to null) } +
            arrayArgsLengths
        )

        val correct =
            if (listener.allocations.isEmpty())
                language["NoneOfTheAbove"] to language["WhatArraySize_NoneOfTheAboveCorrect"].format()
            else
                listener.allocations.first() to null
        val options = correctAndRandomDistractors(correct, distractors.toMap()).toMutableMap()

        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        val statement = language[this::class.simpleName!!].orAnonymous(arguments, procedure)
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