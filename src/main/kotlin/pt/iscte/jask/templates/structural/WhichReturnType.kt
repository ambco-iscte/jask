package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.Expression
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.JAVA_PRIMITIVE_TYPES
import pt.iscte.jask.extensions.getUsedTypes
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.strudel.parsing.java.SourceLocation

class WhichReturnType : StaticQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.getUsedTypes().size >= 2

    companion object {
        fun distractors(method: MethodDeclaration): MutableMap<Option, Boolean> {
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

            return options
        }
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        return Question(
            source,
            TextWithCodeStatement(language["WhichReturnType"].format(method.nameAsString), method),
            distractors(method),
            language = language,
            relevantSourceCode = listOf(SourceLocation(method.type)) // TODO?
        )
    }
}