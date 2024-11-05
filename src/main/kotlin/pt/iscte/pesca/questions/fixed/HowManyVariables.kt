package pt.iscte.pesca.questions.fixed

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.findAll
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.extensions.nameMatches
import pt.iscte.pesca.extensions.prettySignature
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SourceCode
import pt.iscte.pesca.questions.StaticQuestion
import pt.iscte.pesca.questions.TextWithCodeStatement
import kotlin.text.format

data class HowManyVariables(val methodName: String? = null): StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.nameMatches(methodName)

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val method = getApplicableElements<MethodDeclaration>(sources).random()

        val signature = method.prettySignature

        val howManyVariables = method.body.get().findAll<VariableDeclarationExpr>().size

        return QuestionData(
            TextWithCodeStatement(language["HowManyVariables"].format(signature), method.toString()),
            howManyVariables.multipleChoice(language),
            language = language
        )
    }
}