package pt.iscte.pesca.questions

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language

interface QuestionStatement

data class SimpleTextStatement(val statement: String): QuestionStatement {

    override fun toString() = statement
}

data class TextWithCodeStatement(val statement: String, val code: String): QuestionStatement {

    override fun toString() = "$statement${System.lineSeparator()}$code"
}


interface Option

data class SimpleTextOption(val text: String): Option {

    companion object {
        fun none(language: Language = Language.DEFAULT): SimpleTextOption =
            SimpleTextOption(language["NoneOfTheAbove"])

        fun all(language: Language = Language.DEFAULT): SimpleTextOption =
            SimpleTextOption(language["AllOfTheAbove"])

        fun yes(language: Language = Language.DEFAULT): SimpleTextOption =
            SimpleTextOption(language["Yes"])

        fun no(language: Language = Language.DEFAULT): SimpleTextOption =
            SimpleTextOption(language["No"])
    }

    constructor(value: Any): this(value.toString())

    override fun toString() = text
}


data class QuestionData (
    val statement: QuestionStatement,
    private val options: Map<Option, Boolean>,
    val language: Language = Language.DEFAULT,
) {
    private val shuffledOptions: Map<Option, Boolean>
        get() {
            val lastUnshuffled = listOf(
                SimpleTextOption.none(language),
                SimpleTextOption.all(language),
                SimpleTextOption.yes(language),
                SimpleTextOption.no(language)
            )

            val shuffled = options.keys.filter {
                option -> !(lastUnshuffled.contains(option))
            }.shuffled().associateWith {
                option -> options[option]!!
            }.toMutableMap()

            lastUnshuffled.forEach { lastOption ->
                if (options.containsKey(lastOption))
                    shuffled[lastOption] = options[lastOption]!!
            }

            return shuffled
        }

    val solution: List<Option>
        get() = options.filter { it.value }.map { it.key }

    init {
        require(options.size >= 2) { "Question must have at least two options!" }
        require(options.any { option -> option.value }) { "Question must have at least one correct option!" }
    }

    override fun toString(): String = "$statement\n${shuffledOptions.toList().joinToString(System.lineSeparator()) { 
        option -> "[${if (option.second) "x" else " "}] ${option.first}"
    }}"
}


sealed class Question(val range: IntRange = 1 .. Int.MAX_VALUE) {

    constructor(min: Int) : this(min .. Int.MAX_VALUE)

    fun generate(sources: List<String>, language: Language = Language.DEFAULT): QuestionData {
        require(sources.size in range) { "Question should take between ${range.first} and ${range.last} sources!" }
        return build(sources, language)
    }

    protected fun List<String>.getRandomSource(): String =
        filter { isApplicable(it) }.randomOrNull() ?: throw NoSuchElementException("Could not find an applicable source!")

    protected abstract fun build(sources: List<String>, language: Language = Language.DEFAULT): QuestionData

    protected inline fun <reified T : Node> String.getApplicable(condition: (T) -> Boolean = { true }): List<T> =
        flatMap { pt.iscte.pesca.extensions.find<T>(this, condition) }

    protected fun String.findMethod(name: String?): List<MethodDeclaration> =
        flatMap { source ->
            pt.iscte.pesca.extensions.find<MethodDeclaration>(this) { it.nameAsString == name }
        }

    protected inline fun <reified T : Node> List<String>.getApplicable(condition: (T) -> Boolean = { true }): List<T> =
        flatMap { it.getApplicable(condition) }

    protected fun List<String>.findMethod(name: String?): List<MethodDeclaration> =
        flatMap { it.findMethod(name) }

    open fun isApplicable(source: String): Boolean = true
}

abstract class StaticQuestion : Question()

abstract class DynamicQuestion : Question()

