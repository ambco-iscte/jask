package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.model.IProgramElement
import pt.iscte.strudel.parsing.java.JP
import pt.iscte.strudel.parsing.java.SourceLocation
import kotlin.reflect.KClass

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
        val width = code.split("\n").maxOf { it.length }
        val sep = "-".repeat(width)
        return "$statement${System.lineSeparator()}$sep${System.lineSeparator()}$code${System.lineSeparator()}$sep"
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
    val statement: QuestionStatement,
    private val options: Map<Option, Boolean>,
    val language: Language = Language.DEFAULT,
    val choice: QuestionChoiceType = QuestionChoiceType.SINGLE,
    val relevantSourceCode: List<SourceLocation> = emptyList()
) {
    var type: String? = null
        internal set

    var source: ISource? = null
        internal set

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

    init {
        require(options.size >= 2) { "Question must have at least two options!\n$this" }
        require(options.any { option -> option.value }) { "Question must have at least one correct option!\n$this" }
    }

    override fun toString(): String = "$statement\n${options(true).toList().joinToString(System.lineSeparator()) { 
        option -> "[${if (option.second) "x" else " "}] ${option.first}"
    }}"
}