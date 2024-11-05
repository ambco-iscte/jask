package pt.iscte.pesca.questions.fixed

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.findAll
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.extensions.nameMatches
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SourceCode
import pt.iscte.pesca.questions.StaticQuestion
import pt.iscte.pesca.questions.TextWithCodeStatement
import kotlin.text.format

data class HowManyFunctions(val methodName: String? = null): StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.nameMatches(methodName)

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val method = getApplicableElements<MethodDeclaration>(sources).random()

        val otherFunctions = method.findAll<MethodCallExpr>().map { it.nameAsString }.toSet().size

        return QuestionData(
            TextWithCodeStatement(language["HowManyFunctions"].format(method.nameAsString), method),
            otherFunctions.multipleChoice(language),
            language = language
        )
    }
}