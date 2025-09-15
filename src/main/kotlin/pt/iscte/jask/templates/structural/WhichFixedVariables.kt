package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import pt.iscte.jask.Language
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.LiteralExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.UnaryExpr
import pt.iscte.jask.extensions.findAll
import pt.iscte.jask.extensions.getLocalVariables
import pt.iscte.jask.extensions.getUsableVariables
import pt.iscte.jask.extensions.sample
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.strudel.parsing.java.SourceLocation

class WhichFixedVariables : StructuralQuestionTemplate<MethodDeclaration>() {

    private fun MethodDeclaration.getFixedVariables(): List<VariableDeclarator> =
        getLocalVariables().filter { v ->
            // No assign expressions to this variable.
            val noAssigns = findAll(AssignExpr::class.java).none { assign ->
                assign.target.toString() == v.nameAsString
            }

            // And no pesky modifying unary expressions either!
            val noModifies = findAll(UnaryExpr::class.java).none { expr ->
                expr.operator !in setOf(
                    UnaryExpr.Operator.PREFIX_INCREMENT,
                    UnaryExpr.Operator.POSTFIX_INCREMENT,
                    UnaryExpr.Operator.PREFIX_DECREMENT,
                    UnaryExpr.Operator.POSTFIX_DECREMENT
                ) && expr.expression.toString() == v.nameAsString
            }

            noAssigns && noModifies
        }

    // There are fixed value variables (local constants) being used.
    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.getFixedVariables().isNotEmpty()

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val fixedVariables = method.getFixedVariables().toSet()
        val fixedVariablesNames = fixedVariables.map { it.nameAsString }.toSet()

        val localVariables = method.getLocalVariables().map { it.nameAsString }.toSet()
        val notFixedVariables = localVariables.minus(fixedVariablesNames)
        val params = method.parameters.map { it.nameAsString }.toSet()
        val literals = method.findAll(LiteralExpr::class.java).map { it.toString() }

        val randomNotFixed = notFixedVariables.randomOrNull()
        val randomNotFixedAssignment = method.findAll<AssignExpr> {
            it.target is NameExpr && it.target.asNameExpr().nameAsString == randomNotFixed
        }.randomOrNull()?.toString()

        val distractors = sampleSequentially(3, listOf(
            params to language["WhichFixedVariables_DistractorParams"].format(method.nameAsString),
            literals to language["WhichFixedVariables_DistractorLiterals"].format(),
            notFixedVariables to language["WhichFixedVariables_DistractorNotFixed"].format(randomNotFixedAssignment, randomNotFixed),
            localVariables to language["WhichFixedVariables_DistractorAllLocal"].format(randomNotFixedAssignment, randomNotFixed)
        )) {
            it.first.toSet() != fixedVariables.toSet() && it.first.isNotEmpty()
        }

        val options: MutableMap<Option, Boolean> = distractors.associate {
            SimpleTextOption(it.first, it.second) to false
        }.toMutableMap()

        options[SimpleTextOption(
            fixedVariablesNames,
            language["WhichFixedVariables_Correct"].format(fixedVariablesNames.joinToString())
        )] = true

        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return Question(
            source,
            TextWithCodeStatement(
                language["WhichFixedVariables"].orAnonymous(method).format(method.nameAsString),
                method
            ),
            options,
            language = language,
            relevantSourceCode = fixedVariables.map { SourceLocation(it) }
        )
    }
}

fun main() {
    val source = """
        class Test {
            static void foo(int m) {
                int c = 0;
                int d = 0;
                c = c + 1;
                c = c + 1;
                c = c + 1;
            }
        }
    """.trimIndent()

    val template = WhichFixedVariables()
    val qlc = template.generate(source)
    println(qlc)
}