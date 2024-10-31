package pt.iscte.pesca.questions

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.parsing.java.JP

interface QuestionStatement

data class SimpleTextStatement(val statement: String): QuestionStatement {

    override fun toString() = statement
}

data class TextWithCodeStatement(val statement: String, val code: String): QuestionStatement {

    constructor(statement: String, code: MethodDeclaration): this(statement, code.toString())

    constructor(statement: String, code: IProcedureDeclaration): this(statement, (code.getProperty(JP) ?: code).toString())

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


sealed class Question<T : Node>(val range: IntRange = 1 .. Int.MAX_VALUE) {

    companion object {
        init {
            StaticJavaParser.getParserConfiguration().languageLevel = ParserConfiguration.LanguageLevel.JAVA_20
        }
    }

    fun generate(sources: List<String>, language: Language = Language.DEFAULT): QuestionData {
        require(sources.size in range) { "Question should take between ${range.first} and ${range.last} sources!" }
        return build(sources, language)
    }

    protected abstract fun build(sources: List<String>, language: Language = Language.DEFAULT): QuestionData

    inline fun <reified R : T> getApplicableElements(source: String): List<R> =
        pt.iscte.pesca.extensions.find<R>(source) { isApplicable(it) }

    protected inline fun <reified R : T> getApplicableElements(sources: List<String>): List<R> =
        sources.filter { isApplicable<R>(it) }.flatMap {
            source -> pt.iscte.pesca.extensions.find<R>(source) { isApplicable(it) }
        }

    protected inline fun <reified R : T> getApplicableSources(sources: List<String>): List<String> =
        sources.filter { isApplicable<R>(it) }

    inline fun <reified R : T> isApplicable(source: String): Boolean =
        getApplicableElements<R>(source).isNotEmpty()

    open fun isApplicable(element: T): Boolean = true
}

abstract class StaticQuestion<T : Node> : Question<T>()

abstract class DynamicQuestion<T : Node> : Question<T>()

