package pt.iscte.pesca.questions.fixed

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.JAVA_PRIMITIVE_TYPES
import pt.iscte.pesca.extensions.getUsedTypes
import pt.iscte.pesca.extensions.nameMatches
import pt.iscte.pesca.extensions.sample
import pt.iscte.pesca.questions.Option
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.SourceCode
import pt.iscte.pesca.questions.StaticQuestion
import pt.iscte.pesca.questions.TextWithCodeStatement
import kotlin.text.format

data class WhichReturnType(val methodName: String? = null) : StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.nameMatches(methodName) && element.getUsedTypes().size >= 2

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val method = getApplicableElements<MethodDeclaration>(sources).random()

        val methodReturnType = method.type

        val otherTypes = method.getUsedTypes().filter { it != methodReturnType }.map { it.asString() }.toSet()

        val pool = otherTypes + JAVA_PRIMITIVE_TYPES.filter {
            it != methodReturnType.asString()
        }.sample(3 - otherTypes.size)

        val options: MutableMap<Option, Boolean> =
            pool.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(methodReturnType)] = true

        return QuestionData(
            TextWithCodeStatement(language["WhichReturnType"].format(method.nameAsString), method),
            options,
            language = language
        )
    }
}