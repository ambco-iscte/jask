package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import pt.iscte.pesca.Language
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.LiteralExpr
import com.github.javaparser.ast.expr.UnaryExpr
import pt.iscte.pesca.extensions.getLocalVariables
import pt.iscte.pesca.extensions.getUsableVariables
import pt.iscte.pesca.extensions.sample
import pt.iscte.pesca.questions.subtypes.JavaParserQuestionRandomMethod
import pt.iscte.strudel.parsing.java.SourceLocation

class WhichFixedVariables : JavaParserQuestionRandomMethod() {

    private fun MethodDeclaration.getFixedVariables(): List<VariableDeclarator> =
        getLocalVariables().filter { v ->
            // No assign expressions to this variable.
            val noAssigns = findAll(AssignExpr::class.java).none { assign ->
                assign.target.toString() == v.nameAsString
            }

            // And no pesky modifying unary expressions either!
            val noModifies = findAll(UnaryExpr::class.java).none { expr ->
                expr.operator != UnaryExpr.Operator.PLUS && expr.operator != UnaryExpr.Operator.MINUS &&
                expr.expression.toString() == v.nameAsString
            }

            noAssigns && noModifies
        }

    // There are fixed value variables (local constants) being used.
    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.getFixedVariables().isNotEmpty()

    override fun build(source: SourceCode, method: MethodDeclaration, language: Language): QuestionData {
        val fixedVariables = method.getFixedVariables()
        val fixedVariablesNames = fixedVariables.map { it.nameAsString }

        val inScope = method.getUsableVariables().map { it.nameAsString }.toSet()
        val params = method.parameters.map { it.nameAsString }.toSet()
        val literals = method.findAll(LiteralExpr::class.java).map { it.toString() }

        val others = mutableListOf<Set<String>>()
        while (others.size < 3) {
            val choice = (fixedVariablesNames + inScope + params + literals).toSet().sample(null).toSet()
            if (choice != fixedVariablesNames)
                others.add(choice)
        }

        val options: MutableMap<Option, Boolean> =
            others.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(fixedVariablesNames)] = true
        options[SimpleTextOption.none(language)] = false

        return QuestionData(
            source,
            TextWithCodeStatement(language["WhichFixedVariables"].format(method.nameAsString), method),
            options,
            language = language,
            relevantSourceCode = fixedVariables.map { SourceLocation(it) }
        )
    }
}