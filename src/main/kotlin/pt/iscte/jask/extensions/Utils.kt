package pt.iscte.jask.extensions

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.jask.templates.Option
import pt.iscte.jask.templates.SimpleTextOption
import java.lang.reflect.Method

val Class<*>.wrapper: Class<*>
    get() = this.kotlin.javaObjectType

fun MethodDeclaration.accepts(arguments: List<Any>): Boolean {
    val paramTypes = this.parameters.map { it.type }
    if (paramTypes.size != arguments.size)
        return false
    return paramTypes.zip(arguments.map { it::class.java }).all { (parameterType, argumentType) ->
        val parameterTypeName =
            if (parameterType.isPrimitiveType) parameterType.asPrimitiveType().toBoxedType().nameAsString
            else parameterType.asString()
        parameterTypeName == argumentType.wrapper.name || parameterTypeName == argumentType.simpleName
    }
}

val Any?.formatted: String
    get() = when (this) {
        is String -> "\"$this\""    // Strings are placed within "
        is Char -> "'$this'"        // Characters are placed within '
        else -> this.toString()     // Otherwise, value remains unchanged
    }

fun Any.call(methodName: String, arguments: List<Any>): Any? {
    val argumentTypes = arguments.map { it::class.java.wrapper }.toTypedArray()
    val method: Method = this.javaClass.getMethod(methodName, *argumentTypes)
        ?: throw NoSuchMethodException("Method not found: $methodName(${argumentTypes.joinToString { it.simpleName }})")
    return method.invoke(this, *arguments.toTypedArray())
}

fun <T> Collection<T>.sample(amount: Int?): List<T> =
    shuffled().take(amount ?: (1 .. size).random())

fun <K, V> Map<K, V>.sample(amount: Int): Map<K, V> =
    toList().shuffled().take(amount).toMap()

fun <T> Collection<T>.permutations(): Set<List<T>> {
    val permutations = mutableSetOf<List<T>>()

    fun swap(list: MutableList<T>, i: Int, j: Int) {
        val temp = list[i]
        list[i] = list[j]
        list[j] = temp
    }

    fun heap(k: Int, list: MutableList<T>) {
        if (k == 1)
            permutations.add(list.toList())
        else {
            heap(k - 1, list)
            (0 until k - 1).forEach { i ->
                if (k % 2 == 0)
                    swap(list, i, k - 1)
                else
                    swap(list, 0, k - 1)
                heap(k - 1, list)
            }
        }
    }

    heap(this.size, this.toMutableList())

    return permutations
}

fun <T> sampleSequentially(targetSize: Int, vararg collections: Collection<T>, predicate: (T) -> Boolean = { true }): Set<T> {
    require(collections.isNotEmpty())

    val valid = collections.flatMap { it.filter(predicate) }.toSet()

    val solvable = valid.size >= targetSize

    /*
    require(solvable) {
        "Sampling requires $targetSize elements, but only ${valid.size} satisfy the given predicate:\n${valid.joinToString("\n")}"
    }
     */

    val result = mutableSetOf<T>()

    fun step() {
        collections.forEach {
            result.addAll(it.minus(result).filter(predicate).sample(targetSize))
            if (result.size >= targetSize)
                return@forEach
        }
    }

    if (solvable) {
        while (result.size < targetSize) {
            step()
        }
    } else {
        System.err.println("Sampling requires $targetSize elements, but only ${valid.size} satisfy the given " +
                "predicate:\n${valid.joinToString("\n")}")
        step()
    }

    return result.take(targetSize).toSet()
}

fun correctAndRandomDistractors(correct: Pair<Any, String?>, distractors: Map<Any, String?>, maxDistractors: Int = 3): Map<Option, Boolean> =
    mapOf(SimpleTextOption(correct.first, correct.second) to true) +
    distractors
    .filter { it.key != correct.first }
    .sample(maxDistractors).map { SimpleTextOption(it.key, it.value) to false }.toMap()

fun correctAndRandomDistractors(correct: Any, distractors: Set<Any>, maxDistractors: Int = 3): Map<Option,Boolean> =
    correctAndRandomDistractors(correct to null, distractors.associateWith { null }, maxDistractors)

fun <T> Collection<T>.randomBy(predicate: (T) -> Boolean): T =
    filter { predicate(it) }.random()

fun <T> Collection<T>.randomByOrNull(predicate: (T) -> Boolean): T? =
    filter { predicate(it) }.randomOrNull()

fun <T> success(body: () -> T): Boolean =
    runCatching(body).isSuccess

fun <T> failure(body: () -> T): Boolean =
    runCatching(body).isFailure

fun <T, R> Collection<T>.toSetBy(map: (T) -> R): Set<T> {
    val result = mutableSetOf<T>()
    val setBy = mutableSetOf<R>()
    toList().forEach { element ->
        if (setBy.add(map(element)))
            result.add(element)
    }
    return result
}

