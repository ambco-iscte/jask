package pt.iscte.pesca.questions.fixed

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.getLocalVariables
import pt.iscte.pesca.extensions.getVariablesInScope
import pt.iscte.pesca.extensions.sample
import pt.iscte.pesca.questions.Option
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.subtypes.JavaParserQuestionRandomMethod
import kotlin.collections.plus
import kotlin.collections.toSet

class WhichParameters : JavaParserQuestionRandomMethod() {

    override fun build(method: MethodDeclaration, language: Language): QuestionData {
        val parameters = method.parameters.map { it.nameAsString  }.toSet()

        val inScope = method.getVariablesInScope().map { it.nameAsString }.toSet()
        val localVars = method.getLocalVariables().map { it.nameAsString }.toSet()
        val name = method.nameAsString

        val others = mutableListOf<Set<String>>()
        while (others.size < 3) {
            val choice = (parameters + inScope + localVars + setOf(name)).toSet().sample(null).toSet()
            if (choice != parameters)
                others.add(choice)
        }

        val options: MutableMap<Option, Boolean> =
            others.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(parameters)] = true
        options[SimpleTextOption.none(language)] = false

        return QuestionData(
            TextWithCodeStatement(language["WhichParameters"].format(method.nameAsString), method),
            options,
            language = language
        )
    }
}