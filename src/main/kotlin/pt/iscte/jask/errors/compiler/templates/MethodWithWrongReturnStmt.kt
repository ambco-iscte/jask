package pt.iscte.jask.errors.compiler.templates

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.WrongReturnStmtType
import pt.iscte.jask.extensions.nameWithScope
import pt.iscte.jask.templates.*
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.jask.templates.structural.*
import pt.iscte.jask.templates.dynamic.*
import pt.iscte.jask.templates.quality.*

class MethodWithWrongReturnStmt(
    private val error: WrongReturnStmtType? = null
): StaticQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        if (error == null)
            CompilerErrorFinder(element).findReturnStmtsWithWrongType().isNotEmpty()
        else
            element == error.method

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val error = this.error ?: CompilerErrorFinder(method).findReturnStmtsWithWrongType().random()

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