package pt.iscte.pesca.questions.compiler

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.Expression
import pt.iscte.pesca.Language
import pt.iscte.pesca.compiler.ErrorFinder
import pt.iscte.pesca.extensions.JAVA_PRIMITIVE_TYPES
import pt.iscte.pesca.extensions.nameWithScope
import pt.iscte.pesca.extensions.getUsedTypes
import pt.iscte.pesca.extensions.sampleSequentially
import pt.iscte.pesca.questions.Option
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.SourceCode
import pt.iscte.pesca.questions.StaticQuestion
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.strudel.parsing.java.SourceLocation

class MethodWithWrongReturnStmt: StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        ErrorFinder(element).findReturnStmtsWithWrongType().isNotEmpty()

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): QuestionData {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val error = ErrorFinder(method).findReturnStmtsWithWrongType().random()

        val methodReturnType = method.type

        val otherTypes = method.getUsedTypes().map { it.asString() }

        val exprTypes = method.findAll(Expression::class.java).filter { expression ->
            runCatching { expression.calculateResolvedType() }.isSuccess
        }.map { it.calculateResolvedType().describe() }

        val distractors = sampleSequentially(3, otherTypes, exprTypes, listOf(method.nameAsString), JAVA_PRIMITIVE_TYPES) {
            it != methodReturnType.asString() && it != methodReturnType.toString()
        }

        val options: MutableMap<Option, Boolean> =
            distractors.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(methodReturnType)] = true

        return QuestionData(
            source,
            TextWithCodeStatement(language["MethodWithWrongReturnStmt"].format(error.returnStmt.toString(), method.nameWithScope()), method),
            options,
            language = language,
            relevantSourceCode = listOf(SourceLocation(method.type), SourceLocation(error.returnStmt))
        )
    }
}