package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.findAll
import pt.iscte.jask.extensions.multipleChoice
import pt.iscte.strudel.parsing.java.SourceLocation

class HowManyFunctionDependencies : StaticQuestionTemplate<MethodDeclaration>() {

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val otherFunctions =  method.findAll<MethodCallExpr>()
        val otherFunctionsNames = otherFunctions
            .filter { it.nameAsString != method.nameAsString }
            .map { it.nameAsString }.toSet().size

        return Question(
            source,
            TextWithCodeStatement(language[this::class.simpleName!!].format(method.nameAsString), method),
            otherFunctionsNames.multipleChoice(language),
            language = language,
            relevantSourceCode = otherFunctions.map { SourceLocation(it) }
        )
    }
}