package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import jdk.jfr.Description
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getLocalVariables
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.extensions.toSetBy
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.QuestionOption
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.strudel.parsing.java.SourceLocation
import kotlin.collections.plus

class WhichParametersSingleChoice : StructuralQuestionTemplate<MethodDeclaration>() {

    @Description("Method must have at least 1 parameter or local variable")
    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.parameters.isNotEmpty() || element.getLocalVariables().isNotEmpty()

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val parameters = method.parameters.map { it.nameAsString }.toSet()
        val paramTypes = method.parameters.map { it.typeAsString }.toSet()

        val localVars = method.getLocalVariables().map { it.nameAsString }.toSet()
        val localVarTypes = method.getLocalVariables().map { it.typeAsString }.toSet()

        val distractors = sampleSequentially(3, listOf(
            localVars to language["WhichParametersSingleChoice_DistractorLocalVars"].format(method.nameAsString),
            localVars.plus(parameters) to null,
        ), listOf(
            parameters.plus(method.nameAsString) to language["WhichParametersSingleChoice_DistractorParamAndName"].format(),
            localVars.plus(method.nameAsString) to null,
        ), listOf(
            paramTypes to language["WhichParametersSingleChoice_DistractorParamTypes"].format(),
            localVarTypes to language["WhichParametersSingleChoice_DistractorLocalVarTypes"].format(method.nameAsString)
        )) {
            it.first != parameters && it.first.isNotEmpty()
        }.toSetBy { it.first }

        val options: MutableMap<QuestionOption, Boolean> = distractors.associate {
            SimpleTextOption(it.first, it.second) to false
        }.toMutableMap()

        if (parameters.isNotEmpty())
            options[SimpleTextOption(parameters)] = true

        if (parameters.isEmpty() || options.size < 4)
            options[SimpleTextOption.none(
                language,
                if (parameters.isEmpty()) language["WhichParametersSingleChoice_NoneCorrect"].format(method.nameAsString) else null
            )] = parameters.isEmpty()

        return Question(
            source,
            TextWithCodeStatement(language["WhichParameters"].format(method.nameAsString), method),
            options,
            language = language,
            relevantSourceCode = method.parameters.map { SourceLocation(it) }
        )
    }
}