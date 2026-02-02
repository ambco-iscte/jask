package pt.iscte.jask.errors.compiler.templates

import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.type.Type
import pt.iscte.jask.Language
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.UnknownType
import pt.iscte.jask.extensions.JAVA_PRIMITIVE_TYPES
import pt.iscte.jask.extensions.findAllTypes
import pt.iscte.jask.extensions.randomBy
import pt.iscte.jask.extensions.sample
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.QuestionChoiceType
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.templates.StructuralQuestionTemplate
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.strudel.parsing.java.SourceLocation

class ReferencesUndefinedClass(
    private val error: UnknownType? = null
): StructuralQuestionTemplate<TypeDeclaration<*>>() {

    init {
        if (error != null)
            require(error.types.isNotEmpty())
    }

    override fun isApplicable(element: TypeDeclaration<*>): Boolean =
        if (error == null)
            CompilerErrorFinder(element).findUnknownClasses().any { it.types.isNotEmpty() }
        else
            element == error.location || element.isAncestorOf(error.location)

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): Question {
        val (source, unit) = sources.getRandom<TypeDeclaration<*>>()

        val errors = this.error?.let { listOf(it) } ?: CompilerErrorFinder(unit).findUnknownClasses()

        val (type, location, usable) = errors.randomBy { it.types.isNotEmpty() }

        val unusable: Set<Type> =
            unit.findAllTypes().map { it.second }.minus(usable).toSet()

        val primitives: Set<String> =
            JAVA_PRIMITIVE_TYPES.minus((usable + unusable).map { it.asString() }.toSet()).toSet()

        return Question(
            source = source,
            statement = TextWithCodeStatement(
                language["ReferencesUndefinedClass"].format(type.asString(), location.range.get().begin.line),
                source.code
            ),
            options = // TODO better distractors
                usable.associate { SimpleTextOption(it.asString()) to true } +
                primitives.sample(3).associate { SimpleTextOption(it) to true } +
                unusable.associate { SimpleTextOption(it.asString()) to false },
            language = language,
            choice = QuestionChoiceType.MULTIPLE,
            relevantSourceCode = listOf(SourceLocation(location))
        )
    }
}