package pt.iscte.pt.iscte.pesca.questions

import pt.iscte.pt.iscte.pesca.Language
import pt.iscte.pt.iscte.pesca.Language.ENGLISH
import pt.iscte.pt.iscte.pesca.Language.PORTUGUESE
import java.io.File

interface LocalisedObject {
    val translations : Map<Language, String>

    fun getText(): String = translations[Language.DEFAULT].toString()

    fun getText(language: Language): String =
        (translations[language] ?: translations[Language.DEFAULT]).toString()
}

interface QuestionType : LocalisedObject

data class SimpleTextStatement(override var translations: Map<Language, String>): QuestionType {

    constructor(value: Any): this(mutableMapOf(Language.DEFAULT to value.toString()) )

    constructor(vararg translations: Pair<Language, String>) : this(translations.toMap())

    override fun toString() = getText()
}

interface OptionData : LocalisedObject

data class SimpleTextOptionData(override var translations: Map<Language, String>): OptionData {

    constructor(value: Any): this(mutableMapOf(Language.DEFAULT to value.toString()))

    constructor(vararg translations: Pair<Language, String>) : this(translations.toMap())

    override fun toString() = getText()

}

data class QuestionData (
    val statement: QuestionType,
    private val options: Map<OptionData, Boolean>,
    val language: Language = Language.DEFAULT,
) {
    private val shuffledOptions: Map<OptionData, Boolean>
        get() {
            val shuffled = options.keys.filter {
                option -> !(LAST_UNSHUFFLED_OPTIONS.contains(option))
            }.shuffled().associateWith {
                option -> options[option]!!
            }.toMutableMap()

            LAST_UNSHUFFLED_OPTIONS.forEach { lastOption ->
                if (options.containsKey(lastOption))
                    shuffled[lastOption] = options[lastOption]!!
            }

            return shuffled
        }

    val solution: List<OptionData>
        get() = options.filter { it.value }.map { it.key }

    init {
        require(options.size >= 2) { "Question must have at least two options!" }
        require(solution.isNotEmpty()) { "Question must have at least one correct option!" }
    }

    override fun toString(): String = "${statement.getText(language)}\n${shuffledOptions.toList().joinToString(System.lineSeparator()) { 
        option -> "[${if (option.second) "x" else " "}] ${option.first.getText(language)}"
    }}"
}

sealed interface Question {

    /**
     * Builds the question from the source code.
     * @param source Source code of a Java class.
     */
    fun build(source: String, language: Language = Language.DEFAULT): QuestionData

    /**
     * Builds the question from a Java source code file.
     * @param file A Java source code file.
     */
    fun build(file: File, language: Language = Language.DEFAULT): QuestionData = build(file.readText(), language)

    /**
     * Is the question applicable to the current context?
     */
    // Each question should implement its precondition(s) in this function.
    fun isApplicable(source: String): Boolean = true

    fun isApplicable(file: File): Boolean = isApplicable(file.readText())

    fun buildPT(source: String): QuestionData = build(source, PORTUGUESE)

    fun buildEN(source: String): QuestionData = build(source, ENGLISH)

    fun buildPT(file: File): QuestionData = build(file.readText(), PORTUGUESE)

    fun buildEN(file: File): QuestionData = build(file.readText(), ENGLISH)

}

interface StaticQuestion : Question

interface DynamicQuestion : Question

