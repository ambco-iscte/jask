package pt.iscte.pesca.questions.fixed

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.getLoopControlStructures
import pt.iscte.pesca.extensions.hasLoopControlStructures
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.extensions.nameMatches
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SourceCode
import pt.iscte.pesca.questions.StaticQuestion
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.strudel.parsing.java.extensions.getOrNull
import kotlin.text.format

data class HowManyLoops(val methodName: String? = null): StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.nameMatches(methodName) && element.body.getOrNull?.hasLoopControlStructures() == true

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val method = getApplicableElements<MethodDeclaration>(sources).random()

        val howManyLoops = method.body.get().getLoopControlStructures().size

        return QuestionData(
            TextWithCodeStatement(language["HowManyLoops"].format(method.nameAsString), method),
            howManyLoops.multipleChoice(language),
            language = language
        )
    }
}