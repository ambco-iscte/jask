package pt.iscte.jask.errors.compiler.templates

import com.github.javaparser.ast.body.TypeDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.UnknownVariable
import pt.iscte.jask.extensions.randomBy
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.jask.templates.*

class ReferencesUndefinedVariable(
    private val error: UnknownVariable? = null
): StructuralQuestionTemplate<TypeDeclaration<*>>() {

    init {
        if (error != null)
            require(error.scope.getUsableVariables().size >= 2)
    }

    override fun isApplicable(element: TypeDeclaration<*>): Boolean =
        if (error == null)
            CompilerErrorFinder(element).findUnknownVariables().any { it.scope.getUsableVariables().size >= 2 }
        else
            element.isAncestorOf(error.expr)

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): Question {
        val (source, unit) = sources.getRandom<TypeDeclaration<*>>()

        val errors = this.error?.let { listOf(it) } ?: CompilerErrorFinder(unit).findUnknownVariables()

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