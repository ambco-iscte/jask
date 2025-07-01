package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getLocalVariables
import kotlin.collections.plus

class WhichParameterTypes : StructuralQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.parameters.isNotEmpty() || element.getLocalVariables().isNotEmpty()

    companion object {
        fun getDistractors(method: MethodDeclaration, language: Language): Map<Option, Boolean> {
            val parameters = method.parameters.map { it.nameAsString }
            val paramTypes = method.parameters.map { it.typeAsString } // TODO can be equal to one of the others ugghh
            val returnType = method.typeAsString

            val localVars = method.getLocalVariables().map { it.nameAsString }
            val localVarTypes = method.getLocalVariables().map { it.typeAsString }

            val methodName = method.nameAsString

            val options = (if (parameters.isNotEmpty() && localVars.isNotEmpty()) // Method has both parameters and local variables.
                mapOf(
                    SimpleTextOption(parameters) to false,
                    SimpleTextOption(paramTypes) to true,
                    SimpleTextOption(returnType) to false,
                    SimpleTextOption(parameters + localVars) to false
                )
                else if (parameters.isNotEmpty()) // Method only has parameters.
                    mapOf(
                        SimpleTextOption(parameters) to false,
                        SimpleTextOption(parameters + listOf(methodName)) to false,
                        SimpleTextOption(paramTypes) to true,
                        SimpleTextOption(returnType + listOf(methodName)) to false,
                    )
                else if (localVars.isNotEmpty()) // Method only has local variables.
                    mapOf(
                        SimpleTextOption(localVars) to false,
                        SimpleTextOption(localVars + listOf(methodName)) to false,
                        SimpleTextOption(localVarTypes) to false,
                        SimpleTextOption(language["FunctionTakesNoParameters"]) to true,
                    )
                else
                    emptyMap() // This case is never applied, as per the isApplicable method.
            ).toMutableMap()

            options[SimpleTextOption(paramTypes)] = true

            if (options.size < 4)
                options[SimpleTextOption.none(language)] = false

            return options.toMap()
        }
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()
        return Question(
            source,
            TextWithCodeStatement(language["WhichParameterTypes"].format(method.nameAsString), method),
            getDistractors(method, language),
            language = language
        )
    }
}