package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.findAll
import pt.iscte.jask.extensions.multipleChoice
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.jask.extensions.isCallFor
import pt.iscte.jask.extensions.nameWithScope
import pt.iscte.strudel.parsing.java.SourceLocation

class HowManyFunctionDependencies : StructuralQuestionTemplate<MethodDeclaration>() {

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val otherFunctions =  method.findAll<MethodCallExpr>()
        val otherFunctionsNames = otherFunctions
            .filter { !it.isCallFor(method) }
            .map { it.nameWithScope() }.toSet().size

        return Question(
            source,
            TextWithCodeStatement(language[this::class.simpleName!!].format(method.nameAsString), method),
            otherFunctionsNames.multipleChoice(language),
            language = language,
            relevantSourceCode = otherFunctions.map { SourceLocation(it) }
        )
    }
}