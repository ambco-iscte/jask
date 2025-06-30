package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getLocalVariables
import pt.iscte.jask.extensions.getUsableVariables
import pt.iscte.jask.extensions.sample
import pt.iscte.strudel.parsing.java.SourceLocation
import kotlin.collections.plus
import kotlin.collections.toSet

class WhatVariables: StaticQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.getLocalVariables().isNotEmpty()

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val variables = method.getLocalVariables()
        val variableNames = variables.map { it.nameAsString  }.toSet()

        val inScope = method.getUsableVariables().map { it.nameAsString }.toSet()
        val params = method.parameters.map { it.nameAsString }.toSet()
        val name = method.nameAsString

        val others = mutableListOf<Set<String>>()
        while (others.size < 3) {
            val choice = (variableNames + inScope + params + setOf(name)).toSet().sample(null).toSet()
            if (choice != variableNames)
                others.add(choice)
        }

        val options: MutableMap<Option, Boolean> =
            others.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(variableNames)] = true
        options[SimpleTextOption.none(language)] = false

        return Question(
            source,
            TextWithCodeStatement(language["WhatVariables"].format(method.nameAsString), method),
            options,
            language = language,
            relevantSourceCode = variables.map { SourceLocation(it) }
        )
    }
}