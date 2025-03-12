package pt.iscte.pesca.templates

import com.github.javaparser.ast.body.TypeDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.errors.CompilerErrorFinder
import pt.iscte.pesca.extensions.randomBy
import pt.iscte.strudel.parsing.java.SourceLocation

class ReferencesUndefinedVariable : StaticQuestionTemplate<TypeDeclaration<*>>() {

    override fun isApplicable(element: TypeDeclaration<*>): Boolean =
        CompilerErrorFinder(element).findUnknownVariables().any { it.scope.getUsableVariables().size >= 2 }

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): Question {
        val (source, unit) = sources.getRandom<TypeDeclaration<*>>()

        val errors = CompilerErrorFinder(unit).findUnknownVariables()

        val (name, scope) = errors.randomBy { it.scope.getUsableVariables().size >= 2 }

        val usable: Set<String> = scope.getUsableVariables()
        val unusable: Set<String> = scope.enclosed.flatMap { it.getUsableVariables() }.minus(usable).toSet()

        return Question(
            source = source,
            statement = TextWithCodeStatement(
                language["ReferencesUndefinedVariable"].format(name.nameAsString, name.range.get().begin.line),
                source.code
            ),
            options = // TODO better distractors
                usable.associate { SimpleTextOption(it) to true } +
                unusable.associate { SimpleTextOption(it) to false },
            language = language,
            choice = QuestionChoiceType.MULTIPLE,
            relevantSourceCode = listOf(SourceLocation(name))
        )
    }
}