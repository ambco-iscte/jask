package pt.iscte.pesca.questions.compiler

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.compiler.ErrorFinder
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.extensions.randomBy
import pt.iscte.pesca.extensions.relativeTo
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SourceCode
import pt.iscte.pesca.questions.StaticQuestion
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.WhichParameterTypes
import pt.iscte.strudel.parsing.java.SourceLocation

class CallMethodWithWrongParameterTypes: StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        ErrorFinder(element).findMethodCallsWithWrongArguments().any {
            it.parameterTypeMismatch && !it.parameterNumberMismatch
        }

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): QuestionData {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val errors = ErrorFinder(method).findMethodCallsWithWrongArguments()

        val (callTarget, call) = errors.randomBy { it.parameterTypeMismatch && !it.parameterNumberMismatch }

        val line = call.range.get().begin.relativeTo(method.range.get().begin).line

        return QuestionData(
            source,
            TextWithCodeStatement(language["CallMethodWithWrongParameterTypes"].format(
                call.toString(), line, callTarget.nameAsString
            ), NodeList(method, callTarget)),
            WhichParameterTypes.getDistractors(callTarget, language),
            language = language,
            relevantSourceCode = method.parameters.map { SourceLocation(it) }
        )
    }
}