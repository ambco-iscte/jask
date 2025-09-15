package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getLocalVariables
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.strudel.parsing.java.SourceLocation
import kotlin.collections.plus

class WhichParametersSingleChoice : StructuralQuestionTemplate<MethodDeclaration>() {

    // Method has at least one parameter or local variable.
    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.parameters.isNotEmpty() || element.getLocalVariables().isNotEmpty()

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val parameters = method.parameters.map { it.nameAsString }.toSet()
        val paramTypes = method.parameters.map { it.typeAsString }.toSet()

        val localVars = method.getLocalVariables().map { it.nameAsString }.toSet()
        val localVarTypes = method.getLocalVariables().map { it.typeAsString }.toSet()

        val methodName = method.nameAsString

        val distractors = sampleSequentially(3, listOf(
            parameters.plus(method.nameAsString) to language["WhichParametersSingleChoice_DistractorParamAndName"].format(),
            paramTypes to language["WhichParametersSingleChoice_DistractorParamTypes"].format(),
            localVars to language["WhichParametersSingleChoice_DistractorLocalVars"].format(method.nameAsString),
            localVarTypes to language["WhichParametersSingleChoice_DistractorLocalVarTypes"].format(method.nameAsString)
        )) {
            it.first != parameters && it.first.isNotEmpty()
        }

        val options: MutableMap<Option, Boolean> = distractors.associate {
            SimpleTextOption(it.first, it.second) to false
        }.toMutableMap()

        options[SimpleTextOption(parameters)] = true

        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return Question(
            source,
            TextWithCodeStatement(language["WhichParameters"].format(method.nameAsString), method),
            options,
            language = language,
            relevantSourceCode = method.parameters.map { SourceLocation(it) }
        )
    }
}