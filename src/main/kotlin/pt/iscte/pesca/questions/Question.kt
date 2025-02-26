package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.model.IProgramElement
import pt.iscte.strudel.parsing.java.JP
import pt.iscte.strudel.parsing.java.SourceLocation

sealed interface QuestionStatement {
    val statement: String
}

data class SimpleTextStatement(override val statement: String): QuestionStatement {

    override fun toString() = statement
}

data class TextWithCodeStatement(override val statement: String, val code: String): QuestionStatement {

    constructor(statement: String, code: MethodDeclaration): this(statement, code.toString())

    constructor(statement: String, code: IProgramElement): this(statement, (code.getProperty(JP) ?: code).toString())

    constructor(statement: String, code: Collection<IProgramElement>): this(
        statement,
        code.filter{ it.getProperty(JP) != null }.joinToString(System.lineSeparator().repeat(2)) {
            (it.getProperty(JP) ?: it).toString()
        }
    )

    override fun toString(): String {
        val split = code.split("\n")

        val width = split.maxOf { it.length }
        val sep = "-".repeat(width)

        val lines = split.size
        val lineNumberWidth = "$lines.".length

        var i = 1
        val sourceCode = split.joinToString(System.lineSeparator()) { line ->
            val lineNumber = "${i++}."
            val blank = " ".repeat(lineNumberWidth - lineNumber.length + 1)
            "$lineNumber$blank$line"
        }

        return "$statement${System.lineSeparator()}$sep${System.lineSeparator()}$sourceCode${System.lineSeparator()}$sep"
    }
}

data class TextWithMultipleCodeStatements(override val statement: String, val codeStatements: List<String>): QuestionStatement {

    //constructor(statement: String, codeStatements: List<MethodDeclaration>): this(statement, codeStatements.map { it.toString() })

    //constructor(statement: String, codeStatements: List<IProcedureDeclaration>): this(statement, codeStatements.map { (it.getProperty(JP) ?: it).toString() })

    override fun toString() = "$statement${System.lineSeparator()}${codeStatements.map { "$it${System.lineSeparator()}" }}"

    companion object {
        fun from(statement: String, codeStatements: List<IProcedureDeclaration>) =
            TextWithMultipleCodeStatements(statement, codeStatements.map { (it.getProperty(JP) ?: it).toString() })
    }
}


sealed interface Option

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

    constructor(value: Any?): this(when (value) {
        is Collection<*> -> value.joinToString()
        else -> value.toString()
    })

    override fun toString() = text
}

enum class QuestionChoiceType {
    SINGLE,
    MULTIPLE
}

data class QuestionData (
    val source: SourceCode,
    val statement: QuestionStatement,
    private val options: Map<Option, Boolean>,
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
        options: Map<Option, Boolean>,
        language: Language = Language.DEFAULT,
        choice: QuestionChoiceType = QuestionChoiceType.SINGLE,
        relevantSourceCode: List<SourceLocation> = emptyList()
    ) : this(source, statement, options, language, choice, relevantSourceCode) {
        this.questionType = type
    }

    init {
        require(options.size >= 2) { "Question must have at least two options!\n$this" }
        require(options.any { option -> option.value }) { "Question must have at least one correct option!\n$this" }
    }

    fun options(shuffled: Boolean = false): Map<Option, Boolean> {
        val lastUnshuffled = listOf(
            SimpleTextOption.none(language),
            SimpleTextOption.all(language),
            SimpleTextOption.yes(language),
            SimpleTextOption.no(language)
        )

        val shuffled = options.keys.filter {
                option -> !lastUnshuffled.contains(option)
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

    override fun toString(): String = "$statement\n${options(true).toList().joinToString(System.lineSeparator()) { 
        option -> "[${if (option.second) "x" else " "}] ${option.first}"
    }}"
}