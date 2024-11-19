package pt.iscte.pesca.questions.fixed

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.getReturnVariables
import pt.iscte.pesca.extensions.getVariablesInScope
import pt.iscte.pesca.extensions.sample
import pt.iscte.pesca.questions.Option
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.subtypes.JavaParserQuestionRandomMethod
import pt.iscte.pesca.questions.TextWithCodeStatement
import kotlin.jvm.optionals.toSet

class WhichVariableHoldsReturn : JavaParserQuestionRandomMethod() {

    // Return type is given by a single variable.
    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.getReturnVariables().any { it.value.toSet().size == 1 }

    override fun build(method : MethodDeclaration, language: Language): QuestionData {
        val returns = method.getReturnVariables()
        val returnStmt = returns.keys.random()
        val returnVariable = returns[returnStmt]!!.random().nameAsString

        val others = (
            method.getVariablesInScope().map { it.nameAsString } +  // Variables in scope
            method.parameters.map { it.nameAsString } +             // Function parameters
            returns.map { it.key.expression.toSet() }               // Return expressions
        ).filter { it != returnVariable }.toSet().sample(3)

        val options: MutableMap<Option, Boolean> =
            others.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(returnVariable)] = true
        options[SimpleTextOption.none(language)] = false

        return QuestionData(
            TextWithCodeStatement(language["WhichVariableHoldsReturn"].format(method.nameAsString), method),
            options,
            language = language
        )
    }
}