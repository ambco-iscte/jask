package pt.iscte.pesca.extensions

import com.github.javaparser.ast.Node
import pt.iscte.pesca.Language
import pt.iscte.pesca.questions.Option
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.IBlock.IVisitor
import pt.iscte.strudel.model.IType
import pt.iscte.strudel.parsing.java.JP
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.reflect.KClass

val VARIABLE_ROLES = listOf(
    "FixedValue",
    "Gatherer",
    "ArrayIndexIterator",
    "Stepper",
    "MostWantedHolder",
    "OneWayFlag"
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