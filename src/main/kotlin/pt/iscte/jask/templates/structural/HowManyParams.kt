package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.multipleChoice
import pt.iscte.strudel.parsing.java.SourceLocation

class HowManyParams : StructuralQuestionTemplate<MethodDeclaration>() {

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