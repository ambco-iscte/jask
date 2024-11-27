package pt.iscte.pesca.extensions

import pt.iscte.pesca.Language
import pt.iscte.pesca.questions.Option
import pt.iscte.pesca.questions.RecordTypeData
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.IType
import pt.iscte.strudel.model.roles.*
import pt.iscte.strudel.model.roles.impl.*
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.allocateStringArray
import pt.iscte.strudel.parsing.java.extensions.getString
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.NULL
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
fun Any?.toIValue(vm: IVirtualMachine, module: IModule): IValue {

    return when (this) {
        is Collection<*> -> {
            if (isEmpty()) vm.allocateArrayOf(ANY)
            else if (runCatching { vm.getValue(first()) }.isSuccess) {
                val type = vm.getValue(first()).type
                vm.allocateArrayOf(type, *this.map { it.toIValue(vm, module) }.toTypedArray())
            } else if (first() is String) {
                vm.allocateStringArray(*(this as Collection<String>).toTypedArray())
            } else {
                val typeName = when (val f = first()) {
                    is RecordTypeData -> f.name
                    else -> f!!::class.java.canonicalName
                }
                val type =
                    module.types.firstOrNull { it.id == typeName } ?:
                    throw java.lang.UnsupportedOperationException("Cannot find array base type: $typeName")
                vm.allocateArrayOf(type, *this.map { it.toIValue(vm, module) }.toTypedArray())
            }
        }
        is IValue -> this
        is String -> getString(this)
        is RecordTypeData -> {
            val recordType = module.getRecordType(this.name)
            val ref = vm.allocateRecord(recordType)
            val record = ref.target
            recordType.fields.forEachIndexed { i, field ->
                record.setField(field, this.fields[i].toIValue(vm, module))
            }
            ref
        }
        null -> NULL
        else -> vm.getValue(this)
    }
}

fun IValue.multipleChoice(language: Language): Map<Option, Boolean> = when (this.type) {
    INT -> toInt().multipleChoice(language)
    DOUBLE -> toDouble().multipleChoice(language)
    CHAR -> toChar().multipleChoice(language)
    BOOLEAN -> toBoolean().trueOrFalse(language, true)
    else -> throw IllegalArgumentException("Cannot generate multiple choice options for non-primitive value: $this")
}

interface IModuleVisitor {

    fun visit(type: IType): Boolean = true

    fun endVisit(type: IType) { }

    fun visit(constant: IConstantDeclaration): Boolean = true

    fun visit(procedure: IProcedureDeclaration): Boolean = true

    fun endVisit(procedure: IProcedureDeclaration) { }
}

fun IModule.accept(visitor: IModuleVisitor) {
    constants.forEach { visitor.visit(it) }
    types.forEach {
        visitor.visit(it)
        visitor.endVisit(it)
    }
    procedures.forEach {
        visitor.visit(it)
        visitor.endVisit(it)
    }
}

fun IProcedure.getProcedureCalls(): List<IProcedureCall> {
    val lst = mutableListOf<IProcedureCall>()
    val v = object : IBlock.IVisitor {
        override fun visit(call: IProcedureCall): Boolean {
            lst.add(call)
            return true
        }
        override fun visit(exp: IProcedureCallExpression): Boolean {
            lst.add(exp)
            return true
        }
    }
    block.accept(v)
    return lst
}

fun IProcedure.isSelfContained(): Boolean =
    getProcedureCalls().none { it.procedure != this && it.procedure.module == this.module }

fun IProcedure.getUsedProceduresWithinModule(): List<IProcedureDeclaration> =
    getProcedureCalls().filter { it.procedure.module == this.module }.map { it.procedure }

fun IProcedure.getVariableAssignments(): Map<IVariableDeclaration<*>, List<IVariableAssignment>> {
    val map = mutableMapOf<IVariableDeclaration<*>, List<IVariableAssignment>>()
    val v = object : IBlock.IVisitor {
        override fun visit(assignment: IVariableAssignment): Boolean {
            map[assignment.target] = (map[assignment.target] ?: emptyList()) + listOf(assignment)
            return true
        }
    }
    block.accept(v)
    return map
}

fun IProcedure.getLiteralExpressions(): List<ILiteral> {
    val literals = mutableListOf<ILiteral>()
    block.accept(object : IBlock.IVisitor {
        override fun visitAny(exp: IExpression) {
            if (exp is ILiteral)
                literals.add(exp)
        }
    })
    return literals
}
