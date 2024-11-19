package pt.iscte.pesca.questions.fixed

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.findAll
import pt.iscte.pesca.extensions.trueOrFalse
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.subtypes.JavaParserQuestionRandomMethod
import kotlin.text.format

class CallsOtherFunctions : JavaParserQuestionRandomMethod() {

    override fun build(method: MethodDeclaration, language: Language): QuestionData {
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