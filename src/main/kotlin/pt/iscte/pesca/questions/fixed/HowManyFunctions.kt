package pt.iscte.pesca.questions.fixed

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.findAll
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.subtypes.JavaParserQuestionRandomMethod
import kotlin.text.format

class HowManyFunctions : JavaParserQuestionRandomMethod() {

    override fun build(method: MethodDeclaration, language: Language): QuestionData {
        val otherFunctions = method.findAll<MethodCallExpr>().map { it.nameAsString }.toSet().size

        return QuestionData(
            TextWithCodeStatement(language["HowManyFunctions"].format(method.nameAsString), method),
            otherFunctions.multipleChoice(language),
            language = language
        )
    }
}