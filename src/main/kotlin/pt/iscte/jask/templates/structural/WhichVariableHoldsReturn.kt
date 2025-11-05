package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.LiteralExpr
import com.github.javaparser.ast.stmt.ReturnStmt
import jdk.jfr.Description
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getReturnVariables
import pt.iscte.jask.extensions.getUsableVariables
import pt.iscte.jask.extensions.getUsedTypes
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.parsing.java.extensions.getOrNull

class WhichVariableHoldsReturn : StructuralQuestionTemplate<MethodDeclaration>() {

    @Description("Method must return exactly the same 1 variable by itself in all return statements")
    override fun isApplicable(element: MethodDeclaration): Boolean {
        val returns = element.findAll(ReturnStmt::class.java)
        return returns.isNotEmpty() && returns.all {
            it.expression.getOrNull?.isNameExpr == true
        } && returns.map { it.expression?.toString() }.toSet().size == 1
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val returns = method.getReturnVariables()
        val returnStmt = returns.keys.random()
        val returnVariable = returns[returnStmt]!!.random()
        val returnVariableName = returnVariable.nameAsString
        val literals = method.findAll(LiteralExpr::class.java).map { it.toString() }

        val distractors = sampleSequentially(3,
            method.getUsableVariables().map { it.nameAsString },
            method.parameters.map { it.nameAsString },
            returns.map { it.key.expression }.filter { it.getOrNull != returnStmt.expression.getOrNull }.map { it.toString() },
            setOf(method.nameAsString),
            method.getUsedTypes().map { it.asString() }.toSet(),
            literals
        ) {
            it != returnVariableName
        }

        val options: MutableMap<Option, Boolean> =
            distractors.associate { SimpleTextOption(it) to false }.toMutableMap()

        options[SimpleTextOption(returnVariableName)] = true

        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return Question(
            source,
            TextWithCodeStatement(language["WhichVariableHoldsReturn"].format(method.nameAsString), method),
            options,
            language = language,
            relevantSourceCode = listOf(SourceLocation(returnStmt))
        )
    }
}