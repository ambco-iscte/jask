package pt.iscte.pesca.questions.compiler

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.errors.CompilerErrorFinder
import pt.iscte.pesca.extensions.nameWithScope
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SourceCode
import pt.iscte.pesca.questions.StaticQuestion
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.WhichReturnType
import pt.iscte.strudel.parsing.java.SourceLocation

class MethodWithWrongReturnStmt: StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        CompilerErrorFinder(element).findReturnStmtsWithWrongType().isNotEmpty()

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): QuestionData {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val error = CompilerErrorFinder(method).findReturnStmtsWithWrongType().random()

        return QuestionData(
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