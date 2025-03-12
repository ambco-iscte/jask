package pt.iscte.pesca.templates

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.errors.CompilerErrorFinder
import pt.iscte.pesca.extensions.nameWithScope
import pt.iscte.strudel.parsing.java.SourceLocation

class MethodWithWrongReturnStmt: StaticQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        CompilerErrorFinder(element).findReturnStmtsWithWrongType().isNotEmpty()

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val error = CompilerErrorFinder(method).findReturnStmtsWithWrongType().random()

        return Question(
            source,
            TextWithCodeStatement(language["MethodWithWrongReturnStmt"].format(
                error.returnStmt.toString(),
                method.nameWithScope()),
                method
            ),
            WhichReturnType.distractors(method),
            language = language,
            relevantSourceCode = listOf(SourceLocation(method.type), SourceLocation(error.returnStmt))
        )
    }
}