package pt.iscte.pesca.questions.fixed

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.accepts
import pt.iscte.pesca.extensions.trueOrFalse
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SourceCode
import pt.iscte.pesca.questions.StaticQuestion
import pt.iscte.pesca.questions.TextWithCodeStatement
import kotlin.text.format

// FIXME ficava mais clean com dynamic question strudel eu acho
data class CanCallAMethodWithGivenArguments(val methodName: String? = null, val arguments: List<Any>): StaticQuestion<MethodDeclaration>() {

    constructor(methodName: String?, vararg arguments: Any) : this(methodName, arguments.toList())

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val method = getApplicableElements<MethodDeclaration>(sources).random()

        val canCallAMethodWithGivenArguments = method.accepts(arguments)

        val args = arguments.joinToString()

        return QuestionData(
            TextWithCodeStatement(language["CanCallAMethodWithGivenArguments"].format(method.nameAsString, args), method),
            canCallAMethodWithGivenArguments.trueOrFalse(language),
            language = language
        )
    }
}