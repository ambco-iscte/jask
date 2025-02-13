package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.getLocalVariables
import pt.iscte.pesca.questions.subtypes.JavaParserQuestionRandomMethod
import pt.iscte.strudel.parsing.java.SourceLocation
import kotlin.collections.plus

class WhichParametersSingleChoice : JavaParserQuestionRandomMethod() {

    // Method has at least one parameter or local variable.
    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.parameters.isNotEmpty() || element.getLocalVariables().isNotEmpty()

    override fun build(source: SourceCode, method: MethodDeclaration, language: Language): QuestionData {
        val parameters = method.parameters.map { it.nameAsString }
        val paramTypes = method.parameters.map { it.typeAsString }

        val localVars = method.getLocalVariables().map { it.nameAsString }
        val localVarTypes = method.getLocalVariables().map { it.typeAsString }

        val methodName = method.nameAsString

        val options: Map<Option, Boolean> =
            if (parameters.isNotEmpty() && localVars.isNotEmpty()) // Method has both parameters and local variables.
                mapOf(
                    SimpleTextOption(parameters) to true,
                    SimpleTextOption(paramTypes) to false,
                    SimpleTextOption(localVars) to false,
                    SimpleTextOption(parameters + localVars) to false
                )
            else if (parameters.isNotEmpty()) // Method only has parameters.
                mapOf(
                    SimpleTextOption(parameters) to true,
                    SimpleTextOption(parameters + listOf(methodName)) to false,
                    SimpleTextOption(paramTypes) to false,
                    SimpleTextOption(paramTypes + listOf(methodName)) to false,
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

        return QuestionData(
            source,
            TextWithCodeStatement(language["WhichParameters"].format(method.nameAsString), method),
            options,
            language = language,
            relevantSourceCode = method.parameters.map { SourceLocation(it) }
        )
    }
}