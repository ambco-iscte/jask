package pt.iscte.jask.extensions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.templates.Option
import pt.iscte.jask.templates.Question
import pt.iscte.jask.templates.QuestionTemplate
import pt.iscte.jask.templates.SimpleTextOption
import pt.iscte.jask.templates.StructuralQuestionTemplate
import pt.iscte.jask.templates.quality.IFReturnCondition
import pt.iscte.jask.templates.quality.LonelyVariable
import pt.iscte.jask.templates.quality.RemoveEmptyIfAndElse
import pt.iscte.jask.templates.quality.ReplacesIFsWithIfElse
import pt.iscte.jask.templates.quality.UnnecessaryCodeAfterReturn
import pt.iscte.jask.templates.quality.UnnecessaryEqualsTrueOrFalse
import pt.iscte.jask.templates.quality.UnnecessaryIfNesting
import pt.iscte.jask.templates.quality.UnnecessaryParameter
import pt.iscte.jask.templates.quality.UselessDuplicationIfElse
import pt.iscte.jask.templates.quality.UselessDuplicationInsideIfElse
import pt.iscte.jask.templates.quality.UselessSelfAssign
import pt.iscte.jask.templates.quality.UselessVariableDeclaration
import java.lang.reflect.Method
import kotlin.reflect.KClass

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
    } else step()

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

fun <K, V> Map<K, V>.randomKeyBy(predicate: (Map.Entry<K, V>) -> Boolean): K =
    entries.filter(predicate).random().key

fun <K, V> Map<K, V>.randomValueBy(predicate: (Map.Entry<K, V>) -> Boolean): V =
    entries.filter(predicate).random().value

fun <K, V> Map<K, V>.randomKeyByOrNull(predicate: (Map.Entry<K, V>) -> Boolean): K? =
    entries.filter(predicate).randomOrNull()?.key

fun <K, V> Map<K, V>.randomValueByOrNull(predicate: (Map.Entry<K, V>) -> Boolean): V? =
    entries.filter(predicate).randomOrNull()?.value

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


fun checkForApplicables(
    templates: Set<StructuralQuestionTemplate<MethodDeclaration>>,
    source: String
): Set<StructuralQuestionTemplate<MethodDeclaration>> {
    val cu = StaticJavaParser.parse(source)
    val methodDecl = cu.findAll(MethodDeclaration::class.java).firstOrNull() ?: return emptySet()

    return templates.filter { it.isApplicable(methodDecl) }.toSet()
}

fun applicableQualityTemplates(source: String): Set<StructuralQuestionTemplate<MethodDeclaration>> {
    val all = setOf<StructuralQuestionTemplate<MethodDeclaration>>(
        IFReturnCondition(),
        LonelyVariable(),
        RemoveEmptyIfAndElse(),
        ReplacesIFsWithIfElse(),
        UnnecessaryCodeAfterReturn(),
        UnnecessaryEqualsTrueOrFalse(),
        UnnecessaryIfNesting(),
        UnnecessaryParameter(),
        UselessDuplicationIfElse(),
        UselessDuplicationInsideIfElse(),
        UselessSelfAssign(),
        UselessVariableDeclaration()
    )
    return checkForApplicables(all, source)
}

fun generateQuestions(
    templates: Set<StructuralQuestionTemplate<MethodDeclaration>>,
    source: String,
    language: Language = Language.DEFAULT
): List<Question> =
    templates.map { it.generate(source, language) }

