package pt.iscte.pt.iscte.pesca

import java.io.File

interface QuestionType{
    var languageToOptionMapping : Map<String,String>

    fun getText()=languageToOptionMapping[DEFAULT_LANGUAGE].toString()
    fun getText(language: String):String{
        if (!languageToOptionMapping.containsKey(language)){
            return languageToOptionMapping[DEFAULT_LANGUAGE].toString()
        }
        return languageToOptionMapping[language].toString()

    }

}

data class SimpleTextStatement(
    override var languageToOptionMapping: Map<String,String>
): QuestionType {
    constructor(value: Any): this(mutableMapOf(DEFAULT_LANGUAGE to value.toString()) )

    override fun toString() = languageToOptionMapping[DEFAULT_LANGUAGE].toString()
}


interface OptionData{
    var languageToOptionMapping : Map<String,String>

    fun getText(language: String): String {
        if (!languageToOptionMapping.containsKey(language)){
            return languageToOptionMapping[DEFAULT_LANGUAGE].toString()
        }
        return languageToOptionMapping[language].toString()
    }
    fun getText() = languageToOptionMapping[DEFAULT_LANGUAGE].toString()
}

data class SimpleTextOptionData(
    override var languageToOptionMapping: Map<String, String>
): OptionData {
    constructor(value: Any): this(mutableMapOf(DEFAULT_LANGUAGE to value.toString()) )

    override fun toString() = languageToOptionMapping[DEFAULT_LANGUAGE].toString()

}



data class QuestionData(
    val statement: QuestionType,
    private val options: Map<OptionData, Boolean>,
    val language: String = DEFAULT_LANGUAGE,
) {
    private val shuffledOptions: Map<OptionData, Boolean>
        get() {
            val shuffled = options.keys.filter { option -> !(LAST_UNSHUFFLED_OPTIONS.contains(option)) }.shuffled().associateWith {
                option -> options[option]!!
            }.toMutableMap()
            LAST_UNSHUFFLED_OPTIONS.forEach { lastOption ->
                if (options.containsKey(lastOption))
                    shuffled[lastOption] = options[lastOption]!!
            }
            return shuffled
        }

    init {
        require(options.size >= 2) { "Question must have at least two options!" }
        require(options.any { option -> option.value }) { "Question must have at least one correct option!" }
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
    fun build(source: String, language: String = DEFAULT_LANGUAGE): QuestionData

    /**
     * Builds the question from a Java source code file.
     * @param file A Java source code file.
     */
    fun build(file: File, language: String = DEFAULT_LANGUAGE): QuestionData = build(file.readText(), language)

    fun buildPT(source: String): QuestionData = build(source, PORTUGUESE_LANGUAGE)

    fun buildEN(source: String): QuestionData = build(source, ENGLISH_LANGUAGE)

    fun buildPT(file: File): QuestionData = build(file.readText(), PORTUGUESE_LANGUAGE)

    fun buildEN(file: File): QuestionData = build(file.readText(), ENGLISH_LANGUAGE)

}

interface StaticQuestion : Question

interface DynamicQuestion : Question

