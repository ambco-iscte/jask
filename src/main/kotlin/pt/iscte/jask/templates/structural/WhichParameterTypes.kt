package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getLocalVariables
import pt.iscte.jask.extensions.sampleSequentially

class WhichParameterTypes : StructuralQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.parameters.isNotEmpty() || element.getLocalVariables().isNotEmpty()

    companion object {
        fun options(method: MethodDeclaration, language: Language): Map<Option, Boolean> {
            val parameters = method.parameters.map { it.nameAsString }.toSet()
            val paramTypes = method.parameters.map { it.typeAsString }.toSet()
            val returnType = method.typeAsString

            val localVars = method.getLocalVariables().map { it.nameAsString }.toSet()
            val localVarTypes = method.getLocalVariables().map { it.typeAsString }.toSet()

            val methodName = method.nameAsString

            val distractors = sampleSequentially(3, listOf(
                parameters to language["WhichParameterTypes_DistractorParameters"].format(method.nameAsString),
                localVars to language["WhichParameterTypes_DistractorLocalVars"].format(method.nameAsString),
                localVarTypes to language["WhichParameterTypes_DistractorLocalVarTypes"].format(method.nameAsString),
                paramTypes.plus(returnType) to language["WhichParameterTypes_DistractorParamTypesAndReturnType"].format(method.nameAsString, returnType),
                paramTypes.plus(methodName) to null,
                listOf(returnType, methodName) to null
            )) {
                it.first != paramTypes && it.first.isNotEmpty()
            }

            val options: MutableMap<Option, Boolean> = distractors.associate {
                SimpleTextOption(it.first, it.second) to false
            }.toMutableMap()

            if (paramTypes.isNotEmpty())
                options[SimpleTextOption(paramTypes)] = true

            if (paramTypes.isEmpty() || options.size < 4)
                options[SimpleTextOption.none(
                    language,
                    if (paramTypes.isEmpty()) language["WhichParameterTypes_NoneCorrect"].format(method.nameAsString) else null
                )] = paramTypes.isEmpty()

            return options.toMap()
        }
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()
        return Question(
            source,
            TextWithCodeStatement(language["WhichParameterTypes"].format(method.nameAsString), method),
            options(method, language),
            language = language
        )
    }
}