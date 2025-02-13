package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.findAll
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.questions.subtypes.JavaParserQuestionRandomMethod
import pt.iscte.strudel.parsing.java.SourceLocation

class HowManyFunctionDependencies : JavaParserQuestionRandomMethod() {

    override fun build(source: SourceCode, method: MethodDeclaration, language: Language): QuestionData {
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