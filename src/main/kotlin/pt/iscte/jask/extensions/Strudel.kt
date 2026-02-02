package pt.iscte.jask.extensions

import pt.iscte.jask.Language
import pt.iscte.jask.common.QuestionOption
import pt.iscte.jask.common.RecordTypeData
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.IType
import pt.iscte.strudel.parsing.java.allocateStringArray
import pt.iscte.strudel.parsing.java.extensions.getString
import pt.iscte.strudel.parsing.java.extensions.hasThisParameter
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IRecord
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.NULL
import kotlin.reflect.KClass
import kotlin.reflect.cast

@Suppress("UNCHECKED_CAST")
fun Any?.toIValue(vm: IVirtualMachine, module: IModule): IValue {
    return when (this) {
        is Collection<*> -> {
            if (isEmpty()) vm.allocateArrayOf(ANY)
            else if (runCatching { vm.getValue(first()) }.isSuccess) {
                var type = vm.getValue(first()).type
                if (type.isNumber && any { it is Double }) // Ugly hack for e.g. listOf(1, 0.5, 0.25)
                    type = DOUBLE
                vm.allocateArrayOf(type, *this.map { it.toIValue(vm, module) }.toTypedArray())
            }
            else if (first() is List<*>) {
                val innerArrays = this.map { (it as List<*>).toIValue(vm, module) }
                val innerTypes = innerArrays.map { it.type }.toSet()
                val baseType = if (innerTypes.size == 1) innerTypes.first() else ANY
                vm.allocateArrayOf(baseType, *innerArrays.toTypedArray())
            }
            else if (first() is String) {
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
        is Int -> vm.getValue(this)
        is Double -> vm.getValue(this)
        is Boolean -> vm.getValue(this)
        is Char -> vm.getValue(this)
        else -> vm.getValue(this)
    }
}

fun IValue.multipleChoice(language: Language): Map<QuestionOption, Boolean> = when (this.type) {
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
            map[assignment.target] = (map[assignment.target] ?: emptyList()).plus(assignment)
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

fun IProcedure.countArrayAccesses(): Int {
    var count = 0
    block.accept(object : IBlock.IVisitor {
        override fun visit(exp: IArrayAccess): Boolean {
            count++
            return true
        }

        override fun visit(assignment: IArrayElementAssignment): Boolean {
            count++
            return true
        }

        override fun visit(call: IProcedureCall): Boolean {
            if (call.procedure is IProcedure)
                count += (call.procedure as IProcedure).countArrayAccesses()
            return true
        }
    })
    return count
}

fun <T : IProgramElement> IBlockHolder.deepFindAll(type: KClass<T>): List<T> {
    val all = mutableListOf<T>()
    val find  = object : IBlock.IVisitor {
        override fun visitAny(element: IBlockElement) {
            if (type.isInstance(element))
                all.add(type.cast(element))
        }

        override fun visitAny(exp: IExpression) {
            if (type.isInstance(exp))
                all.add(type.cast(exp))
        }
    }
    block.accept(find)
    return all
}

inline fun <reified T : IProgramElement> IBlockHolder.deepFindAll(): List<T> =
    this.deepFindAll(T::class)

fun <T : IProgramElement> IExpression.findAll(type: KClass<T>): List<T> {
    val found = mutableListOf<T>()
    this.accept(object: IExpression.IVisitor {
        override fun visitAny(exp: IExpression) {
            if (type.isInstance(exp))
                found.add(type.cast(exp))
        }
    })
    return found
}

inline fun <reified T : IProgramElement> IExpression.findAll(): List<T> =
    this.findAll(T::class)

internal fun Collection<Any?>.toIValues(vm: IVirtualMachine, module: IModule): List<IValue> =
    map { it.toIValue(vm, module) }

// Pretty print :)
internal fun IValue.asString(): String = when (this@asString) {
    is IReference<*> -> target.asString()
    is IRecord -> "new $this"
    is IArray -> "[${elements.joinToString { it.asString() }}]"
    else -> toString()
}

internal fun Any?.toStringPretty(): String = when (this@toStringPretty) {
    is IValue -> asString()
    is String -> "\"$this\""
    is Char -> "'$this'"
    is Collection<*> -> "[${joinToString { it.toStringPretty() }}]"
    else -> toString()
}

internal fun Collection<Any?>.joinToStringPretty(): String = joinToString { it.toStringPretty() }

fun procedureCallAsString(procedure: IProcedureDeclaration, arguments: List<Any?>): String =
    if (procedure.hasThisParameter)
        "${arguments.first().toStringPretty()}.${procedure.id}(${arguments.subList(1, arguments.size).joinToStringPretty()})"
    else
        "${procedure.id}(${arguments.joinToStringPretty()})"

val IProcedureDeclaration.isMain: Boolean
    get() = parameters.isEmpty() &&
            id == "main" &&
            (parameters.isEmpty() || (
                parameters.size == 1 &&
                parameters[0].type.isArrayReference &&
                (((parameters[0].type as? IReferenceType)?.target as? IArrayType)?.componentType as? HostRecordType)?.type == String::class.java
            ))