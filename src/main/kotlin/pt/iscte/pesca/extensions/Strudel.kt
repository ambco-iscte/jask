package pt.iscte.pesca.extensions

import pt.iscte.pesca.Language
import pt.iscte.pesca.questions.Option
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.IType
import pt.iscte.strudel.model.roles.*
import pt.iscte.strudel.parsing.java.allocateStringArray
import pt.iscte.strudel.parsing.java.extensions.getString
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.NULL
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
fun Any?.toIValue(vm: IVirtualMachine): IValue = when (this) {
    is Collection<*> -> {
        if (isEmpty()) vm.allocateArrayOf(NULL.type)
        else if (runCatching { vm.getValue(first()) }.isSuccess) {
            val type = vm.getValue(first()).type
            vm.allocateArrayOf(type, *this.map { it ?: NULL }.toTypedArray())
        } else if (first() is String) {
            vm.allocateStringArray(*(this as Collection<String>).toTypedArray())
        } else {
            val type = HostRecordType(first()!!::class.java.canonicalName)
            vm.allocateArrayOf(type, *this.map { it ?: NULL }.toTypedArray())
        }
    }
    null -> NULL
    is IValue -> this
    is String -> getString(this)
    else -> vm.getValue(this)
}

val VARIABLE_ROLES: Map<KClass<out IVariableRole>, String> = mapOf(
    IFixedValue::class to "Fixed Value",
    IGatherer::class to "Gatherer",
    IArrayIndexIterator::class to "Array Index Iterator",
    IStepper::class to "Stepper",
    IMostWantedHolder::class to "Most-Wanted Holder",
    IOneWayFlag::class to "One-Way Flag"
)

fun IProcedureDeclaration.generateRandomArguments(vm: IVirtualMachine): List<IValue> =
    parameters.map { it.type }.map { it.generateRandomValue(vm) }

fun IType.generateRandomValue(vm: IVirtualMachine, numRange: IntRange = 0.. 10): IValue = when (this) {
    INT -> vm.getValue(Random.nextInt(numRange))
    DOUBLE -> vm.getValue(Random.nextDouble(numRange.first.toDouble(), numRange.last.toDouble()))
    CHAR -> vm.getValue(('z' downTo 'a').toList().random())
    BOOLEAN -> vm.getValue(listOf(true, false).random())
    else -> throw IllegalArgumentException("Cannot generate random value for non-value type: $this")
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