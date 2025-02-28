package pt.iscte.pesca.questions

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.correctAndRandomDistractors
import pt.iscte.pesca.extensions.procedureCallAsString
import pt.iscte.pesca.extensions.toIValues
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

class WhatArraySize : DynamicQuestion<IProcedure>() {
    var allocation: Int? = null

    override fun isApplicable(element: IProcedure): Boolean =
        element.block.findExpression<IArrayAllocation>() != null

    fun setup(vm: IVirtualMachine) {
        vm.addListener(object : IVirtualMachine.IListener {
            override fun arrayAllocated(ref: IReference<IArray>) {
                // exclude args allocation
                if(!vm.callStack.isEmpty && allocation == null)
                    allocation = ref.target.length
            }
        })
    }

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        setup(vm)
        val arguments = args.toIValues(vm, module)

        vm.execute(procedure, *arguments.toTypedArray())

        val arrayArgsLengths = arguments.filter { it is IReference<*> && it.target is IArray }.map {
            ((it as IReference<*>).target as IArray).length
        }

        val distractors = mutableSetOf<Any>(
            allocation?: language["NoneOfTheAbove"],
            allocation?.minus(1) ?: 0,
            allocation?.plus(1) ?: 0,
        ) + arrayArgsLengths

        return QuestionData(
            source,
            TextWithCodeStatement(
                language[this::class.simpleName!!].format(procedureCallAsString(procedure, arguments)), procedure
            ),
            correctAndRandomDistractors(allocation ?: language["NoneOfTheAbove"], distractors),
            language = language
        )
    }
}