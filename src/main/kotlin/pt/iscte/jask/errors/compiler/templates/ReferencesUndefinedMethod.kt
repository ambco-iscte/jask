package pt.iscte.jask.errors.compiler.templates

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.UnknownMethod
import pt.iscte.jask.extensions.randomBy
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.jask.templates.*
import pt.iscte.jask.templates.structural.*
import pt.iscte.jask.templates.dynamic.*
import pt.iscte.jask.templates.quality.*

class ReferencesUndefinedMethod(
    private val error: UnknownMethod? = null
): StructuralQuestionTemplate<TypeDeclaration<*>>() {

    init {
        if (error != null)
            require(error.usable.size >= 2)
    }

    override fun isApplicable(element: TypeDeclaration<*>): Boolean =
        if (error == null)
            CompilerErrorFinder(element).findUnknownMethods().any { it.usable.size >= 2 }
        else
            element.isAncestorOf(error.call)

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): Question {
        val (source, unit) = sources.getRandom<TypeDeclaration<*>>()

        val errors = this.error?.let { listOf(it) } ?: CompilerErrorFinder(unit).findUnknownMethods()

        val (call, usable) = errors.randomBy { it.usable.size >= 2 }

        val unusable: Set<MethodDeclaration> =
            call.findCompilationUnit().get().findAll(MethodDeclaration::class.java).minus(usable).toSet()

        return Question(
            source = source,
            statement = TextWithCodeStatement(
                language["ReferencesUndefinedMethod"].format(call.nameAsString, call.range.get().begin.line),
                source.code
            ),
            options = // TODO better distractors
                usable.associate { SimpleTextOption(it.nameAsString) to true } +
                unusable.associate { SimpleTextOption(it.nameAsString) to false },
            language = language,
            choice = QuestionChoiceType.MULTIPLE,
            relevantSourceCode = listOf(SourceLocation(call))
        )
    }
}