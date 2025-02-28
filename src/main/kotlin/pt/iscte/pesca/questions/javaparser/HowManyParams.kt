package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.strudel.parsing.java.SourceLocation
import kotlin.text.format

class HowManyParams : StaticQuestion<MethodDeclaration>() {

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val (source, method) = sources.getRandom<MethodDeclaration>()

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