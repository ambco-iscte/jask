package pt.iscte.jask.errors.compiler.templates

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.UnknownMethod
import pt.iscte.jask.errors.compiler.UnknownType
import pt.iscte.jask.extensions.randomBy
import pt.iscte.jask.templates.Question
import pt.iscte.jask.templates.QuestionChoiceType
import pt.iscte.jask.templates.SimpleTextOption
import pt.iscte.jask.templates.SourceCode
import pt.iscte.jask.templates.StructuralQuestionTemplate
import pt.iscte.jask.templates.TextWithCodeStatement
import pt.iscte.strudel.parsing.java.SourceLocation

class ReferencesUndefinedClass(
    private val error: UnknownType? = null
): StructuralQuestionTemplate<TypeDeclaration<*>>() {

    init {
        if (error != null)
            require(error.types.size >= 2)
    }

    override fun isApplicable(element: TypeDeclaration<*>): Boolean =
        if (error == null)
            CompilerErrorFinder(element).findUnknownClasses().any { it.types.size >= 2 }
        else
            element == error.location || element.isAncestorOf(error.location)

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): Question {
        val (source, unit) = sources.getRandom<TypeDeclaration<*>>()

        val errors = this.error?.let { listOf(it) } ?: CompilerErrorFinder(unit).findUnknownClasses()

        val (type, location, usable) = errors.randomBy { it.types.size >= 2 }

        val unusable: Set<TypeDeclaration<*>> =
            location.findCompilationUnit().get().findAll(TypeDeclaration::class.java).minus(usable).toSet()

        return Question(
            source = source,
            statement = TextWithCodeStatement(
                language["ReferencesUndefinedMethod"].format(type.asString(), location.range.get().begin.line),
                source.code
            ),
            options = // TODO better distractors
                usable.associate { SimpleTextOption(it.nameAsString) to true } +
                unusable.associate { SimpleTextOption(it.nameAsString) to false },
            language = language,
            choice = QuestionChoiceType.MULTIPLE,
            relevantSourceCode = listOf(SourceLocation(location))
        )
    }
}