package pt.iscte.jask.errors.compiler.templates

import com.github.javaparser.ast.body.TypeDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.WrongMethodCallParameters
import pt.iscte.jask.extensions.multipleChoice
import pt.iscte.jask.extensions.randomBy
import pt.iscte.jask.extensions.relativeTo
import pt.iscte.jask.templates.*
import pt.iscte.strudel.parsing.java.SourceLocation

class CallMethodWithWrongParameterNumber(
    private val error: WrongMethodCallParameters? = null
): StructuralQuestionTemplate<TypeDeclaration<*>>() {

    init {
        if (error != null)
            require(error.parameterNumberMismatch)
    }

    override fun isApplicable(element: TypeDeclaration<*>): Boolean =
        if (error == null)
            CompilerErrorFinder(element).findMethodCallsWithWrongArguments().any {
                it.parameterNumberMismatch
            }
        else
            element.isAncestorOf(error.call)

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): Question {
        val (source, type) = sources.getRandom<TypeDeclaration<*>>()

        val errors = this.error?.let { listOf(it) } ?: CompilerErrorFinder(type).findMethodCallsWithWrongArguments()

        val (callTarget, call) = errors.randomBy { it.parameterNumberMismatch }

        val line = call.range.get().begin.relativeTo(type.range.get().begin).line

        val parameters = callTarget.parameters.size
        return Question(
            source,
            TextWithCodeStatement(language["CallMethodWithWrongParameterNumber"].format(
                call.toString(), line, callTarget.nameAsString
            ), callTarget.toString()),
            parameters.multipleChoice(language),
            language = language,
            relevantSourceCode = callTarget.parameters.map { SourceLocation(it) } + listOf(SourceLocation(call))
        )
    }
}