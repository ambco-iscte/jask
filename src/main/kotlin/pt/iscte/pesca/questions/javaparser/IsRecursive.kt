package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.findAll
import pt.iscte.pesca.extensions.hasMethodCalls
import pt.iscte.pesca.extensions.trueOrFalse
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.parsing.java.extensions.getOrNull

class IsRecursive : StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.body.getOrNull?.hasMethodCalls() == true

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val recursiveCalls = method.findAll<MethodCallExpr>().filter { call ->
            call.nameAsString == method.nameAsString
        }
        val isRecursive = recursiveCalls.isNotEmpty()

        return QuestionData(
            source,
            TextWithCodeStatement(language["IsRecursive"].format(method.nameAsString), method.toString()),
            isRecursive.trueOrFalse(language),
            language = language,
            relevantSourceCode = recursiveCalls.map { SourceLocation(it) }
        )
    }
}