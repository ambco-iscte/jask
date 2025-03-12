package pt.iscte.pesca.templates

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.strudel.parsing.java.SourceLocation

class HowManyParams : StaticQuestionTemplate<MethodDeclaration>() {

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val parameters = method.parameters.size
        return Question(
            source,
            TextWithCodeStatement(language["HowManyParams"].format(method.nameAsString), method.toString()),
            parameters.multipleChoice(language),
            language = language,
            relevantSourceCode = method.parameters.map { SourceLocation(it) }
        )
    }
}