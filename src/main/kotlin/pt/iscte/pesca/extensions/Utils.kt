package pt.iscte.pesca.extensions

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.questions.Option
import pt.iscte.pesca.questions.Question
import pt.iscte.pesca.questions.SimpleTextOption
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

fun <T> sampleSequentially(targetSize: Int, vararg collections: Collection<T>, predicate: (T) -> Boolean = { true }): Set<T> {
    require(collections.isNotEmpty())
    val result = mutableSetOf<T>()
    collections.forEach {
        result.addAll(it.filter { predicate(it) }.sample(targetSize))
        if (result.size >= targetSize)
            return@forEach
    }
    return result.take(targetSize).toSet()
}

fun correctAndRandomDistractors(correct: Any, distractors: Set<Any>, maxDistractors: Int = 3): Map<Option,Boolean> =
    mapOf(SimpleTextOption(correct) to true) +
    distractors
        .filter { it != correct }
        .sample(maxDistractors).map { Pair(SimpleTextOption(it), false) }

fun <T> Collection<T>.randomBy(predicate: (T) -> Boolean): T =
    filter { predicate(it) }.random()

fun <T> Collection<T>.randomByOrNull(predicate: (T) -> Boolean): T? =
    filter { predicate(it) }.randomOrNull()

