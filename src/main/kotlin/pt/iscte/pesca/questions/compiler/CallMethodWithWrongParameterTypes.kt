package pt.iscte.pesca.questions.compiler

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.TypeDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.errors.CompilerErrorFinder
import pt.iscte.pesca.extensions.randomBy
import pt.iscte.pesca.extensions.relativeTo
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SourceCode
import pt.iscte.pesca.questions.StaticQuestion
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.WhichParameterTypes
import pt.iscte.strudel.parsing.java.SourceLocation

class CallMethodWithWrongParameterTypes: StaticQuestion<TypeDeclaration<*>>() {

    override fun isApplicable(element: TypeDeclaration<*>): Boolean =
        CompilerErrorFinder(element).findMethodCallsWithWrongArguments().any {
            it.parameterTypeMismatch && !it.parameterNumberMismatch
        }

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): QuestionData {
        val (source, type) = sources.getRandom<TypeDeclaration<*>>()

        val errors = CompilerErrorFinder(type).findMethodCallsWithWrongArguments()

        val (callTarget, call) = errors.randomBy { it.parameterTypeMismatch && !it.parameterNumberMismatch }

        val line = call.range.get().begin.relativeTo(type.range.get().begin).line

        return QuestionData(
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