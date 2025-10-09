package pt.iscte.jask.templates.dynamic
import pt.iscte.jask.templates.*

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.correctAndRandomDistractors
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.toIValues
import pt.iscte.strudel.model.IArrayAllocation
import pt.iscte.strudel.model.IBlock
import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
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
        var allocation: Int? = null
            private set

        override fun arrayAllocated(ref: IReference<IArray>) {
            // exclude args allocation
            if(!vm.callStack.isEmpty && allocation == null)
                allocation = ref.target.length
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

        val distractors: Set<Pair<Any, String?>> = mutableSetOf(
            (listener.allocation?.minus(1) ?: 0) to null,
            (listener.allocation?.plus(1) ?: 0) to null,
        ) + arrayArgsLengths

        val statement = language[this::class.simpleName!!].orAnonymous(arguments, procedure)
        return Question(
            source,
            TextWithCodeStatement(
                statement.format(procedureCallAsString(procedure, args)),
                procedure
            ),
            correctAndRandomDistractors(
                (
                    if (listener.allocation == null)
                        language["NoneOfTheAbove"] to language["WhatArraySize_NoneOfTheAboveCorrect"].format()
                    else
                        listener.allocation!! to null
                ),
                distractors.toMap(),
            ),
            language = language
        )
    }
}