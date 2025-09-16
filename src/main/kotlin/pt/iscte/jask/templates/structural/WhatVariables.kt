package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.LiteralExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getLocalVariables
import pt.iscte.jask.extensions.getUsableVariables
import pt.iscte.jask.extensions.isMain
import pt.iscte.jask.extensions.sample
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.strudel.parsing.java.SourceLocation
import kotlin.collections.plus
import kotlin.collections.toSet

class WhatVariables: StructuralQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.getLocalVariables().isNotEmpty() // There is at least 1 local variable

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val variables = method.getLocalVariables()
        val variableNames = variables.map { it.nameAsString  }.toSet()

        val variableTypes = variables.map { it.type.toString() }.toSet()

        val inScope = method.getUsableVariables().map { it.nameAsString }.toSet()
        val params = method.parameters.map { it.nameAsString }.toSet()
        val name = method.nameAsString

        val literals = method.findAll(LiteralExpr::class.java).map { it.toString() }.toSet()

        val distractors = sampleSequentially(3, listOf(
            literals to language["WhatVariables_DistractorLiterals"].format(),
            variableNames.plus(literals) to language["WhatVariables_DistractorVarsAndLiterals"].format(method.nameAsString),
            variableTypes to language["WhatVariables_DistractorVarTypes"].format(method.nameAsString),
        ), (if (method.isMain) emptyList() else listOf(
            inScope to language["WhatVariables_DistractorAllInScope"].format(method.nameAsString),
            params to language["WhatVariables_DistractorParams"].format(method.nameAsString),
            listOf(name) to language["WhatVariables_DistractorMethodName"].format(method.nameAsString),
            listOf(name).plus(params) to language["WhatVariables_DistractorMethodNameAndParams"].format(method.nameAsString),
            listOf(name).plus(inScope) to language["WhatVariables_DistractorNameAndAllInScope"].format(method.nameAsString),
            listOf(name).plus(variableNames) to language["WhatVariables_DistractorNameAndVars"].format()
        ))) {
            it.first.toSet() != variableNames && it.first.isNotEmpty()
        }

        val options: MutableMap<Option, Boolean> = distractors.associate {
            SimpleTextOption(it.first, it.second) to false
        }.toMutableMap()
        options[SimpleTextOption(
            variableNames,
            language["WhatVariables_Correct"].orAnonymous(method).format(
                method.findAll(VariableDeclarationExpr::class.java).firstOrNull()?.toString() ?: "int n = 42;"
            )
        )] = true

        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return Question(
            source,
            TextWithCodeStatement(
                language["WhatVariables"].orAnonymous(method).format(method.nameAsString),
                method
            ),
            options,
            language = language,
            relevantSourceCode = variables.map { SourceLocation(it) }
        )
    }
}

fun main() {
    val source = """
        class Test {
            static void main() {
                int c = 0;
                c = c + 1;
                c = c + 1;
                c = c + 1;
            }
        }
    """.trimIndent()

    val template = WhatVariables()
    val qlc = template.generate(source)
    println(qlc)
}