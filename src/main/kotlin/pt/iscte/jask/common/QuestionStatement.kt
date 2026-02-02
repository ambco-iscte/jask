package pt.iscte.jask.common

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.expr.Expression
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

    constructor(statement: String, code: Node): this(statement, code.toString())

    constructor(statement: String, code: NodeList<Node>): this(statement, code.joinToString(System.lineSeparator().repeat(2)) {
        if (it is Expression && !it.toString().endsWith(";"))
            "$it;"
        else it.toString()
    })

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

        val sourceCode = split.joinToString(System.lineSeparator())

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