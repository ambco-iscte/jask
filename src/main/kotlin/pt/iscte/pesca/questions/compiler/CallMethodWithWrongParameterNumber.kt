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
import pt.iscte.strudel.parsing.java.SourceLocation

class CallMethodWithWrongParameterNumber: StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        ErrorFinder(element).findMethodCallsWithWrongArguments().any { it.parameterNumberMismatch }

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): QuestionData {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val errors = ErrorFinder(method).findMethodCallsWithWrongArguments()

        val (callTarget, call) = errors.randomBy { it.parameterNumberMismatch }

        val line = call.range.get().begin.relativeTo(method.range.get().begin).line

        val parameters = method.parameters.size
        return QuestionData(
            source,
            TextWithCodeStatement(language["CallMethodWithWrongParameterNumber"].format(
                call.toString(), line, callTarget.nameAsString
            ), NodeList(method, callTarget)),
            parameters.multipleChoice(language),
            language = language,
            relevantSourceCode = method.parameters.map { SourceLocation(it) }
        )
    }
}