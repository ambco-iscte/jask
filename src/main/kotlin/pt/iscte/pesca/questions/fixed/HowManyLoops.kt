package pt.iscte.pesca.questions.fixed

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.getLoopControlStructures
import pt.iscte.pesca.extensions.hasLoopControlStructures
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.subtypes.JavaParserQuestionRandomMethod
import pt.iscte.strudel.parsing.java.extensions.getOrNull
import kotlin.text.format

class HowManyLoops : JavaParserQuestionRandomMethod() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.body.getOrNull?.hasLoopControlStructures() == true

    override fun build(method: MethodDeclaration, language: Language): QuestionData {
        val howManyLoops = method.body.get().getLoopControlStructures().size

        return QuestionData(
            TextWithCodeStatement(language["HowManyLoops"].format(method.nameAsString), method),
            howManyLoops.multipleChoice(language),
            language = language
        )
    }
}