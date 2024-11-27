package pt.iscte.pesca.questions.fixed

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.ReturnStmt
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.getReturnVariables
import pt.iscte.pesca.extensions.getVariablesInScope
import pt.iscte.pesca.extensions.sample
import pt.iscte.pesca.extensions.sampleSequentially
import pt.iscte.pesca.questions.Option
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.subtypes.JavaParserQuestionRandomMethod
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.strudel.parsing.java.extensions.getOrNull
import kotlin.jvm.optionals.toSet

class WhichVariableHoldsReturn : JavaParserQuestionRandomMethod() {

    // Return value is given by a single variable.
    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.findAll(ReturnStmt::class.java).all { it.expression.getOrNull?.isNameExpr == true } &&
        element.findAll(ReturnStmt::class.java).map { it.expression?.toString() }.toSet().size == 1

    override fun build(method : MethodDeclaration, language: Language): QuestionData {
        val returns = method.getReturnVariables()
        val returnStmt = returns.keys.random()
        val returnVariable = returns[returnStmt]!!.random().nameAsString

        val distractors = sampleSequentially(3,
            method.getVariablesInScope().map { it.nameAsString },
            method.parameters.map { it.nameAsString },
            returns.map { it.key.expression }.filter { it != returnStmt.expression }.map { it.toString() }
        ) {
            it != returnVariable
        }

        val options: MutableMap<Option, Boolean> =
            distractors.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(returnVariable)] = true
        if (distractors.size < 3)
            options[SimpleTextOption.none(language)] = false

        return QuestionData(
            TextWithCodeStatement(language["WhichVariableHoldsReturn"].format(method.nameAsString), method),
            options,
            language = language
        )
    }
}