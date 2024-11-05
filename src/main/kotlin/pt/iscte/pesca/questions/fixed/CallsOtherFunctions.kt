package pt.iscte.pesca.questions.fixed

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.findAll
import pt.iscte.pesca.extensions.nameMatches
import pt.iscte.pesca.extensions.trueOrFalse
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SourceCode
import pt.iscte.pesca.questions.StaticQuestion
import pt.iscte.pesca.questions.TextWithCodeStatement
import kotlin.text.format

data class CallsOtherFunctions(val methodName: String? = null) : StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.nameMatches(methodName)

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val method = getApplicableElements<MethodDeclaration>(sources).random()

        val callsOtherFunctions = method.findAll<MethodCallExpr>().any { call ->
            call.nameAsString != method.nameAsString
        }

        return QuestionData(
            TextWithCodeStatement(language["CallsOtherFunctions"].format(method.nameAsString), method),
            callsOtherFunctions.trueOrFalse(language),
            language = language
        )
    }
}