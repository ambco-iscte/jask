package pt.iscte.jask.errors.compiler.templates

import com.github.javaparser.ast.expr.MethodCallExpr
import pt.iscte.jask.Language
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.WrongMethodCallParameters
import pt.iscte.jask.extensions.multipleChoice
import pt.iscte.jask.extensions.randomBy
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.jask.extensions.randomKeyBy
import pt.iscte.jask.templates.*
import pt.iscte.strudel.parsing.java.SourceLocation

class HowManyMethodCallArguments(
    private val error: WrongMethodCallParameters? = null
): StructuralQuestionTemplate<MethodCallExpr>() {

    init {
        if (error != null)
            require(error.parameterNumberMismatch)
    }

    override fun isApplicable(element: MethodCallExpr): Boolean =
        if (error == null)
            CompilerErrorFinder(element.findCompilationUnit().get()).findMethodCallsWithWrongArguments().any {
                it.parameterNumberMismatch
            }
        else
            element == error.call

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): Question {
        val (source, type) = sources.getRandom<MethodCallExpr>()

        val errors = this.error?.let { listOf(it) } ?: CompilerErrorFinder(type).findMethodCallsWithWrongArguments()

        val (callTarget, call) = errors.randomBy { it.parameterNumberMismatch }

        val arguments = call.arguments.size

        val options = arguments.multipleChoice(language).toMutableMap()
        if (SimpleTextOption(callTarget.parameters.size) !in options) {
            options.remove(options.randomKeyBy { !it.value })
            options[SimpleTextOption(callTarget.parameters.size)] = false
        }

        return Question(
            source,
            TextWithCodeStatement(
                language["HowManyMethodCallArguments"].format(
                    callTarget.nameAsString,
                    call.toString()
                ), call
            ),
            options,
            language = language,
            relevantSourceCode = callTarget.parameters.map { SourceLocation(it) } + listOf(SourceLocation(call))
        )
    }
}