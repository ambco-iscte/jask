package pt.iscte.jask.errors.compiler

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.Expression
import pt.iscte.jask.errors.ICompilerError
import pt.iscte.jask.extensions.ControlFlowGraph
import kotlin.math.min

class MissingReturnInBranch(
    val method: MethodDeclaration,
    private val flow: ControlFlowGraph,
    val branch: List<ControlFlowGraph.CFGEdge>
): ICompilerError {

    fun condition(): Expression {
        var condition: Expression? = null

        branch.filter { it.condition.isPresent }.forEach { edge ->
            condition = if (condition == null)
                edge.condition.get()
            else
                BinaryExpr(condition, edge.condition.get(), BinaryExpr.Operator.AND)
        }

        return condition ?: BooleanLiteralExpr(true)
    }

    private fun pretty(branch: List<ControlFlowGraph.CFGEdge>): String {
        val builder = StringBuilder()

        fun separator(edge: ControlFlowGraph.CFGEdge): String {
            val width = min(edge.from.toString().length, edge.to.toString().length) / 2
            val spacing = " ".repeat(width)
            return "\n$spacing|\n$spacing|${edge.condition.map { " ($it)" }.orElse("")}\n$spacing↓\n"
        }

        branch.subList(2, branch.size).forEachIndexed { i, edge ->
            builder.append(edge.from)
            builder.append(separator(edge))
            if (edge.to is ControlFlowGraph.EmptyNode)
                builder.append(edge.to)
        }

        return builder.toString()
    }

    override fun message(): String = "Missing return statement:\n${pretty(branch)}"
}