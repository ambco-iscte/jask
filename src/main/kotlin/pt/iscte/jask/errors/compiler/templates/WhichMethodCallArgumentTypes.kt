package pt.iscte.jask.errors.compiler.templates

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.resolution.model.typesystem.LazyType
import com.github.javaparser.resolution.model.typesystem.NullType
import com.github.javaparser.resolution.types.ResolvedReferenceType
import com.github.javaparser.resolution.types.ResolvedType
import pt.iscte.jask.Language
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.WrongMethodCallParameters
import pt.iscte.jask.extensions.randomBy
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.jask.extensions.configureStaticJavaParser
import pt.iscte.jask.extensions.randomKeyBy
import pt.iscte.jask.extensions.simpleName
import pt.iscte.jask.templates.*
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.jask.templates.structural.*
import kotlin.jvm.optionals.getOrNull

class WhichMethodCallArgumentTypes(
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

        val callArgTypes = call.arguments.mapIndexed { index, expression ->
            runCatching {
                expression.calculateResolvedType().simpleName
            }.getOrDefault(callTarget.parameters[index].typeAsString)
        }

        val options = WhichParameterTypes.options(callTarget, language, callArgTypes).toMutableMap()

        val trueParamTypes = SimpleTextOption(callTarget.parameters.map { it.typeAsString })
        if (trueParamTypes !in options) {
            options.remove(options.randomKeyBy { !it.value })
            options[trueParamTypes] = false
        }

        return Question(
            source,
            TextWithCodeStatement(
                language["WhichMethodCallArgumentTypes"].format(
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