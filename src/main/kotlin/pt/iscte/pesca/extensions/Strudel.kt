package pt.iscte.pesca.extensions

import pt.iscte.pesca.Language
import pt.iscte.pesca.questions.Option
import pt.iscte.pesca.questions.RecordTypeData
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.IType
import pt.iscte.strudel.model.roles.*
import pt.iscte.strudel.model.roles.impl.*
import pt.iscte.strudel.parsing.java.allocateStringArray
import pt.iscte.strudel.parsing.java.extensions.getString
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.NULL
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
fun Any?.toIValue(vm: IVirtualMachine, module: IModule): IValue = when (this) {
    is Collection<*> -> {
        if (isEmpty()) vm.allocateArrayOf(ANY)
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
    is RecordTypeData -> {
        val recordType = module.getRecordType(this.name)
        val ref = vm.allocateRecord(recordType)
        val record = ref.target
        recordType.fields.forEachIndexed { i, field ->
            record.setField(field, this.fields[i].toIValue(vm, module))
        }
        ref
    }
    else -> vm.getValue(this)
}

val VARIABLE_ROLES: Map<KClass<out IVariableRole>, String> = mapOf(
    FixedValue::class to "Fixed Value",
    Gatherer::class to "Gatherer",
    ArrayIndexIterator::class to "Array Index Iterator",
    Stepper::class to "Stepper",
    MostWantedHolder::class to "Most-Wanted Holder",
    OneWayFlag::class to "One-Way Flag"
)

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