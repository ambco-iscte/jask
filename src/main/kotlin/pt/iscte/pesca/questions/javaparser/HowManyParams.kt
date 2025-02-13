package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.extensions.prettySignature
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.subtypes.JavaParserQuestionRandomMethod
import pt.iscte.strudel.parsing.java.SourceLocation
import kotlin.text.format

class HowManyParams : JavaParserQuestionRandomMethod() {

    override fun build(source: SourceCode, method: MethodDeclaration, language: Language): QuestionData {
        val parameters = method.parameters.size
        return QuestionData(
            source,
            TextWithCodeStatement(language["HowManyParams"].format(method.nameAsString), method.toString()),
            parameters.multipleChoice(language),
            language = language,
            relevantSourceCode = method.parameters.map { SourceLocation(it) }
        )
    }
}