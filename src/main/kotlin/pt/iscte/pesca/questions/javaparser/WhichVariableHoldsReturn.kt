package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.ReturnStmt
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.getReturnVariables
import pt.iscte.pesca.extensions.getUsableVariables
import pt.iscte.pesca.extensions.sampleSequentially
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.parsing.java.extensions.getOrNull

class WhichVariableHoldsReturn : StaticQuestion<MethodDeclaration>() {

    // Return value is given by a single variable.
    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.findAll(ReturnStmt::class.java).all { it.expression.getOrNull?.isNameExpr == true } &&
        element.findAll(ReturnStmt::class.java).map { it.expression?.toString() }.toSet().size == 1

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val returns = method.getReturnVariables()
        val returnStmt = returns.keys.random()
        val returnVariable = returns[returnStmt]!!.random()
        val returnVariableName = returnVariable.nameAsString

        val distractors = sampleSequentially(3,
            method.getUsableVariables().map { it.nameAsString },
            method.parameters.map { it.nameAsString },
            returns.map { it.key.expression }.filter { it != returnStmt.expression }.map { it.toString() },
            listOf(method.nameAsString)
        ) {
            it != returnVariableName
        }

        val options: MutableMap<Option, Boolean> =
            distractors.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(returnVariableName)] = true
        if (distractors.size < 3)
            options[SimpleTextOption.none(language)] = false

        return QuestionData(
            source,
            TextWithCodeStatement(language["WhichVariableHoldsReturn"].format(method.nameAsString), method),
            options,
            language = language,
            relevantSourceCode = listOf(SourceLocation(returnStmt))
        )
    }
}