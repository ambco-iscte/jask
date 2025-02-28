package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.Expression
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.JAVA_PRIMITIVE_TYPES
import pt.iscte.pesca.extensions.getUsedTypes
import pt.iscte.pesca.extensions.sampleSequentially
import pt.iscte.strudel.parsing.java.SourceLocation

class WhichReturnType : StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.getUsedTypes().size >= 2

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val (source, method) = sources.getRandom<MethodDeclaration>()

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
            TextWithCodeStatement(language["WhichReturnType"].format(method.nameAsString), method),
            options,
            language = language,
            relevantSourceCode = listOf(SourceLocation(method.type)) // TODO?
        )
    }
}