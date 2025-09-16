package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.Expression
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.JAVA_PRIMITIVE_TYPES
import pt.iscte.jask.extensions.getUsedTypes
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.strudel.parsing.java.SourceLocation

class WhichReturnType : StructuralQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.getUsedTypes().size >= 2

    companion object {
        fun options(method: MethodDeclaration, language: Language): MutableMap<Option, Boolean> {
            val otherTypes = method.getUsedTypes().map { it.asString() }

            val exprTypes = method.findAll(Expression::class.java).filter { expression ->
                runCatching { expression.calculateResolvedType() }.isSuccess
            }.map {
                val name = it.calculateResolvedType().describe()
                name.split(".").lastOrNull() ?: name
            }

            val distractors = sampleSequentially(3,
                otherTypes.map { it to null },
                exprTypes.map { it to null },
                listOf(method.nameAsString to language["WhichReturnType_DistractorName"].format()),
                JAVA_PRIMITIVE_TYPES.map { it to null }
            ) {
                it.first != method.type.asString() && it.first != method.type.toString()
            }

            val options: MutableMap<Option, Boolean> = distractors.associate {
                SimpleTextOption(it.first, it.second) to false
            }.toMutableMap()

            options[SimpleTextOption(method.type.asString(), language["WhichReturnType_Correct"].format())] = true

            if (options.size < 4)
                options[SimpleTextOption.none(language)] = false

            return options
        }
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        return Question(
            source,
            TextWithCodeStatement(language["WhichReturnType"].format(method.nameAsString), method),
            options(method, language),
            language = language,
            relevantSourceCode = listOf(SourceLocation(method.type))
        )
    }
}