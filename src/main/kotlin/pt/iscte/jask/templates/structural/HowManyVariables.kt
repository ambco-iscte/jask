package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.correctAndRandomDistractors
import pt.iscte.jask.extensions.getLocalVariables
import pt.iscte.strudel.parsing.java.SourceLocation

class HowManyVariables : StaticQuestionTemplate<MethodDeclaration>() {

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val localVariables = method.getLocalVariables()
        val howManyVariables = localVariables.size

        return Question(
            source,
            TextWithCodeStatement(language["HowManyVariables"].format(method.nameAsString), method.toString()),
            correctAndRandomDistractors(
                howManyVariables,
                setOf(method.parameters.size, method.parameters.size + howManyVariables, 0, 1, 2, 3, 4),
                3
            ),
            language = language,
            relevantSourceCode = localVariables.map { SourceLocation(it) }
        )
    }
}