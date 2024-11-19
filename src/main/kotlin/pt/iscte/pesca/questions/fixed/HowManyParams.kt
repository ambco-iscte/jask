package pt.iscte.pesca.questions.fixed

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.extensions.prettySignature
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.subtypes.JavaParserQuestionRandomMethod
import kotlin.text.format

class HowManyParams : JavaParserQuestionRandomMethod() {

    override fun build(method: MethodDeclaration, language: Language): QuestionData {
        val signature = method.prettySignature
        val parameters = method.parameters.size

        return QuestionData(
            TextWithCodeStatement(language["HowManyParams"].format(signature), method.toString()),
            parameters.multipleChoice(language),
            language = language
        )
    }
}