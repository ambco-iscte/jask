package pt.iscte.jask.common

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.shuffled
import pt.iscte.strudel.parsing.java.SourceLocation

data class QuestionSequenceWithContext(
    val context: QuestionStatement,
    val questions: List<Question>,
    val feedback: String? = null
) {
    constructor(context: QuestionStatement, question: Question, feedback: String? = null):
            this(context, listOf(question), feedback)

    override fun toString(): String =
        "${context}\n\n" + questions.joinToString("\n\n")
}

data class Question (
    val source: SourceCode,
    val statement: QuestionStatement,
    val options: Map<QuestionOption, Boolean>,
    val language: Language = Language.DEFAULT,
    val choice: QuestionChoiceType = QuestionChoiceType.SINGLE,
    val relevantSourceCode: List<SourceLocation> = emptyList()
) {
    lateinit var questionType: String
        internal set

    val hasQuestionType: Boolean
        get() = ::questionType.isInitialized

    constructor(
        type: String,
        source: SourceCode,
        statement: QuestionStatement,
        options: Map<QuestionOption, Boolean>,
        language: Language = Language.DEFAULT,
        choice: QuestionChoiceType = QuestionChoiceType.SINGLE,
        relevantSourceCode: List<SourceLocation> = emptyList()
    ) : this(source, statement, options, language, choice, relevantSourceCode) {
        this.questionType = type
    }

    init {
        require(options.size >= 2) { "Question must have at least two options!\n$this" }
        require(options.any { option -> option.value }) { "Question must have at least one correct option!\n$this" }
        require(options.keys.map { it.toString() }.toSet().size == options.keys.map { it.toString() }.size) {
            "Question has duplicate options!"
        }
    }

    fun options(shuffle: Boolean = false): Map<QuestionOption, Boolean> {
        if (!shuffle)
            return options

        val keepLast = listOf(
            language["NoneOfTheAbove"].toString(),
            language["AllOfTheAbove"].toString(),
            language["Yes"].toString(),
            language["No"].toString()
        )

        val shuffled = options.filter {
            it.key.toString() !in keepLast
        }.shuffled().toMutableMap()

        keepLast.forEach { last ->
            val match = options.keys.firstOrNull { it.toString() == last }
            if (match != null)
                shuffled[match] = options[match]!!
        }

        return shuffled
    }

    val solution: List<QuestionOption>
        get() = options.filter { it.value }.map { it.key }

    override fun toString(): String = "$statement\n${options.toList().joinToString("\n") { (option, correct) ->
        val feedback = option.feedback
        if (feedback == null)
            "[${if (correct) "x" else " "}] $option"
        else
            "[${if (correct) "x" else " "}] $option\t\t-> $feedback"
    }}"
}