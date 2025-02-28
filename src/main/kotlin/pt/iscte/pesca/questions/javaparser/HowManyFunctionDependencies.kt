package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.findAll
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.strudel.parsing.java.SourceLocation

class HowManyFunctionDependencies : StaticQuestion<MethodDeclaration>() {

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val otherFunctions =  method.findAll<MethodCallExpr>()
        val otherFunctionsNames = otherFunctions
            .filter { it.nameAsString != method.nameAsString }
            .map { it.nameAsString }.toSet().size

        return QuestionData(
            source,
            TextWithCodeStatement(language[this::class.simpleName!!].format(method.nameAsString), method),
            otherFunctionsNames.multipleChoice(language),
            language = language,
            relevantSourceCode = otherFunctions.map { SourceLocation(it) }
        )
    }
}