package pt.iscte.pesca.questions.fixed

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.extensions.nameMatches
import pt.iscte.pesca.extensions.prettySignature
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SourceCode
import pt.iscte.pesca.questions.StaticQuestion
import pt.iscte.pesca.questions.TextWithCodeStatement
import kotlin.text.format

data class HowManyParams(val methodName: String? = null): StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.nameMatches(methodName)

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val method = getApplicableElements<MethodDeclaration>(sources).random()

        val signature = method.prettySignature
        val parameters = method.parameters.size

        return QuestionData(
            TextWithCodeStatement(language["HowManyParams"].format(signature), method.toString()),
            parameters.multipleChoice(language),
            language = language
        )
    }
}