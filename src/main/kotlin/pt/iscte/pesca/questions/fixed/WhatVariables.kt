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
import kotlin.collections.filter
import kotlin.collections.plus
import kotlin.collections.toSet
import kotlin.jvm.optionals.toSet

class WhatVariables: JavaParserQuestionRandomMethod() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.getLocalVariables().isNotEmpty()

    override fun build(method: MethodDeclaration, language: Language): QuestionData {
        val variables = method.getLocalVariables().map { it.nameAsString  }.toSet()

        val inScope = method.getVariablesInScope().map { it.nameAsString }.toSet()
        val params = method.parameters.map { it.nameAsString }.toSet()
        val name = method.nameAsString

        val others = mutableListOf<Set<String>>()
        while (others.size < 3) {
            val choice = (variables + inScope + params + setOf(name)).toSet().sample(null).toSet()
            if (choice != variables)
                others.add(choice)
        }

        val options: MutableMap<Option, Boolean> =
            others.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(variables)] = true
        options[SimpleTextOption.none(language)] = false

        return QuestionData(
            TextWithCodeStatement(language["WhatVariables"].format(method.nameAsString), method),
            options,
            language = language
        )
    }
}