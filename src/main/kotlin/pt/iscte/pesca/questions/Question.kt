package pt.iscte.pesca.questions

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.model.IProgramElement
import pt.iscte.strudel.parsing.java.JP

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
        code.joinToString(System.lineSeparator().repeat(2)) { (it.getProperty(JP) ?: it).toString() }
    )

    override fun toString() = "$statement${System.lineSeparator()}$code"
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


data class QuestionData (
    val statement: QuestionStatement,
    val options: Map<Option, Boolean>,
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