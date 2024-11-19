package pt.iscte.pesca.questions.fixed

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.findAll
import pt.iscte.pesca.extensions.hasMethodCalls
import pt.iscte.pesca.extensions.prettySignature
import pt.iscte.pesca.extensions.trueOrFalse
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.subtypes.JavaParserQuestionRandomMethod
import pt.iscte.strudel.parsing.java.extensions.getOrNull
import kotlin.text.format

class IsRecursive : JavaParserQuestionRandomMethod() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.body.getOrNull?.hasMethodCalls() == true

    override fun build(method: MethodDeclaration, language: Language): QuestionData {
        val signature = method.prettySignature

        val isRecursive = method.findAll<MethodCallExpr>().any { call ->
            call.nameAsString == method.nameAsString
        }

        return QuestionData(
            TextWithCodeStatement(language["IsRecursive"].format(signature), method.toString()),
            isRecursive.trueOrFalse(language),
            language = language
        )
    }
}