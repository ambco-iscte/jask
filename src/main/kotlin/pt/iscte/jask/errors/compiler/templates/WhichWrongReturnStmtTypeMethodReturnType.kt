package pt.iscte.jask.errors.compiler.templates

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.WrongReturnStmtType
import pt.iscte.jask.extensions.nameWithScope
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.jask.templates.*
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.jask.templates.structural.*

class WhichWrongReturnStmtTypeMethodReturnType(
    private val error: WrongReturnStmtType? = null
): StructuralQuestionTemplate<MethodDeclaration>() {

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
            TextWithCodeStatement(
                language["WhichReturnType"].format(method.nameWithScope()),
                method
            ),
            WhichReturnType.options(method, language),
            language = language,
            relevantSourceCode = listOf(SourceLocation(method.type), SourceLocation(error.returnStmt))
        )
    }
}