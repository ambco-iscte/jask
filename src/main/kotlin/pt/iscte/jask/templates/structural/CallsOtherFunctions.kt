package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.findAll
import pt.iscte.jask.extensions.findMethodDeclaration
import pt.iscte.jask.extensions.nameWithScope
import pt.iscte.jask.extensions.trueOrFalse
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.jask.extensions.isCallFor
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.parsing.java.extensions.getOrNull

class CallsOtherFunctions : StructuralQuestionTemplate<MethodDeclaration>() {

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val otherFunctions = method.findAll<MethodCallExpr>().filter { call ->
            !call.isCallFor(method)
        }
        val callsOtherFunctions = otherFunctions.isNotEmpty()

        return Question(
            source,
            TextWithCodeStatement(
                language["CallsOtherFunctions"].format(method.nameAsString),
                method
            ),
            callsOtherFunctions.trueOrFalse(language),
            language = language,
            relevantSourceCode = otherFunctions.map { SourceLocation(it) }
        )
    }
}