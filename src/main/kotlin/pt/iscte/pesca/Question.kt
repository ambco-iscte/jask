package pt.iscte.pt.iscte.pesca

import java.io.File

interface QuestionStatement

data class SimpleTextStatement(val text: String): QuestionStatement {
    override fun toString() = text
}


interface OptionData

data class SimpleTextOptionData(val text: String): OptionData {
    constructor(value: Any): this(value.toString())

    override fun toString() = text
}

val NoneOfTheAbove = SimpleTextOptionData("None of the above.")


data class QuestionData(
    val statement: QuestionStatement,
    private val options: Map<OptionData, Boolean>,
) {
    val shuffledOptions: Map<OptionData, Boolean>
        get() {
            val shuffled = options.keys.filter { option -> option != NoneOfTheAbove }.shuffled().associateWith {
                option -> options[option]!!
            }.toMutableMap()
            if (options.containsKey(NoneOfTheAbove))
                shuffled[NoneOfTheAbove] = options[NoneOfTheAbove]!!
            return shuffled
        }

    init {
        require(options.size >= 2) { "Question must have at least two options!" }
        require(options.any { option -> option.value }) { "Question must have at least one correct option!" }
    }

    override fun toString(): String = "$statement\n${shuffledOptions.toList().joinToString(System.lineSeparator()) { 
        option -> "[${if (option.second) "x" else " "}] ${option.first}"
    }}"
}

sealed interface Question {
    /**
     * Builds the question from the source code.
     * @param source Source code of a Java class.
     */
    fun build(source: String): QuestionData
}

interface StaticQuestion : Question

interface DynamicQuestion : Question

/**
 * Builds the question from a Java source code file.
 * @param file A Java source code file.
 */
fun Question.build(file: File): QuestionData = build(file.readText())