package pt.iscte.jask.errors.compiler.templates

import com.github.javaparser.ast.body.VariableDeclarator
import pt.iscte.jask.Language
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.WrongTypeForVariableDeclaration
import pt.iscte.jask.extensions.findAllTypes
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.common.QuestionOption
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.templates.StructuralQuestionTemplate
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.jask.extensions.JAVA_PRIMITIVE_TYPES
import pt.iscte.strudel.parsing.java.SourceLocation

class WhichVariableType(
    private val error: WrongTypeForVariableDeclaration? = null
): StructuralQuestionTemplate<VariableDeclarator>() {

    init {
        if (error != null)
            require(error.initialiserIsMethodCall)
    }

    override fun isApplicable(element: VariableDeclarator): Boolean =
        if (error == null)
            CompilerErrorFinder(element.findCompilationUnit().get()).findVariablesAssignedWithWrongType().isNotEmpty()
        else
            element == error.variable

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): Question {
        val (source, unit) = sources.getRandom<VariableDeclarator>()

        val error = this.error ?: CompilerErrorFinder(unit).findVariablesAssignedWithWrongType().random()

        val distractors = sampleSequentially(3, unit.findAllTypes().map {
            it.first.toString()
        }, JAVA_PRIMITIVE_TYPES) {
            it != error.actual.describe()
        }

        val options: MutableMap<QuestionOption, Boolean> = distractors.associate {
            SimpleTextOption(it, null) to false
        }.toMutableMap()
        options[SimpleTextOption(error.variable.typeAsString)] = true

        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return Question(
            source,
            TextWithCodeStatement(
                language["WhichVariableType"].format(error.variable, error.variable.range.get().begin.line),
                source.code
            ),
            options,
            language = language,
            relevantSourceCode = listOf(SourceLocation(error.variableInitialiser), SourceLocation(error.variable))
        )
    }
}