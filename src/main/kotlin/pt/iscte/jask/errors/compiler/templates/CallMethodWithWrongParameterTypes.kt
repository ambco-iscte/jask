package pt.iscte.jask.errors.compiler.templates

import com.github.javaparser.ast.expr.MethodCallExpr
import pt.iscte.jask.Language
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.WrongMethodCallParameters
import pt.iscte.jask.extensions.randomBy
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.jask.templates.*
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.jask.templates.structural.*

class CallMethodWithWrongParameterTypes(
    private val error: WrongMethodCallParameters? = null
): StructuralQuestionTemplate<MethodCallExpr>() {

    init {
        if (error != null)
            require(error.parameterTypeMismatch && !error.parameterNumberMismatch)
    }

    override fun isApplicable(element: MethodCallExpr): Boolean =
        if (error == null)
            CompilerErrorFinder(element.findCompilationUnit().get()).findMethodCallsWithWrongArguments().any {
                it.parameterTypeMismatch && !it.parameterNumberMismatch
            }
        else
            element == error.call

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): Question {
        val (source, type) = sources.getRandom<MethodCallExpr>()

        val errors = this.error?.let { listOf(it) } ?: CompilerErrorFinder(type).findMethodCallsWithWrongArguments()

        val (callTarget, call) = errors.randomBy { it.parameterTypeMismatch && !it.parameterNumberMismatch }

        return Question(
            source,
            TextWithCodeStatement(
                language["WhichParameterTypes"].format(
                    callTarget.nameAsString
                ), callTarget.toString()
            ),
            WhichParameterTypes.options(callTarget, language),
            language = language,
            relevantSourceCode = callTarget.parameters.map { SourceLocation(it) } + listOf(SourceLocation(call))
        )
    }
}