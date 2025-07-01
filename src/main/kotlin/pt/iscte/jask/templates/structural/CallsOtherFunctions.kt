package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.findAll
import pt.iscte.jask.extensions.trueOrFalse
import pt.iscte.strudel.parsing.java.SourceLocation

class CallsOtherFunctions : StructuralQuestionTemplate<MethodDeclaration>() {

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val otherFunctions = method.findAll<MethodCallExpr>().filter {call ->
            call.nameAsString != method.nameAsString
        }
        val callsOtherFunctions = otherFunctions.isNotEmpty()

        return Question(
            source,
            TextWithCodeStatement(language["CallsOtherFunctions"].format(method.nameAsString), method),
            callsOtherFunctions.trueOrFalse(language),
            language = language,
            relevantSourceCode = otherFunctions.map { SourceLocation(it) }
        )
    }
}