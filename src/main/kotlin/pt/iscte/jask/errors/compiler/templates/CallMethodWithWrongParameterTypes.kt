package pt.iscte.jask.errors.compiler.templates

import com.github.javaparser.ast.body.TypeDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.WrongMethodCallParameters
import pt.iscte.jask.extensions.randomBy
import pt.iscte.jask.extensions.relativeTo
import pt.iscte.jask.templates.*
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.jask.templates.structural.*
import pt.iscte.jask.templates.dynamic.*
import pt.iscte.jask.templates.quality.*

class CallMethodWithWrongParameterTypes(
    private val error: WrongMethodCallParameters? = null
): StaticQuestionTemplate<TypeDeclaration<*>>() {

    init {
        if (error != null)
            require(error.parameterTypeMismatch && !error.parameterNumberMismatch)
    }

    override fun isApplicable(element: TypeDeclaration<*>): Boolean =
        if (error == null)
            CompilerErrorFinder(element).findMethodCallsWithWrongArguments().any {
                it.parameterTypeMismatch && !it.parameterNumberMismatch
            }
        else
            element.isAncestorOf(error.call)

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): Question {
        val (source, type) = sources.getRandom<TypeDeclaration<*>>()

        val errors = this.error?.let { listOf(it) } ?: CompilerErrorFinder(type).findMethodCallsWithWrongArguments()

        val (callTarget, call) = errors.randomBy { it.parameterTypeMismatch && !it.parameterNumberMismatch }

        val line = call.range.get().begin.relativeTo(type.range.get().begin).line

        return Question(
            source,
            TextWithCodeStatement(language["CallMethodWithWrongParameterTypes"].format(
                call.toString(), line, callTarget.nameAsString
            ), callTarget.toString()),
            WhichParameterTypes.getDistractors(callTarget, language),
            language = language,
            relevantSourceCode = callTarget.parameters.map { SourceLocation(it) } + listOf(SourceLocation(call))
        )
    }
}