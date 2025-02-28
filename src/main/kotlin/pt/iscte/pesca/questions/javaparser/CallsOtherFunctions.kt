package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.findAll
import pt.iscte.pesca.extensions.trueOrFalse
import pt.iscte.strudel.parsing.java.SourceLocation

class CallsOtherFunctions : StaticQuestion<MethodDeclaration>() {

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val otherFunctions = method.findAll<MethodCallExpr>().filter {call ->
            call.nameAsString != method.nameAsString
        }
        val callsOtherFunctions = otherFunctions.isNotEmpty()

        return QuestionData(
            source,
            TextWithCodeStatement(language["CallsOtherFunctions"].format(method.nameAsString), method),
            callsOtherFunctions.trueOrFalse(language),
            language = language,
            relevantSourceCode = otherFunctions.map { SourceLocation(it) }
        )
    }
}