package pt.iscte.pesca.extensions

import pt.iscte.pesca.Language
import pt.iscte.pesca.questions.Option
import pt.iscte.strudel.model.*
import pt.iscte.strudel.model.IType
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.random.Random
import kotlin.random.nextInt

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