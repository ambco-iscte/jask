package pt.iscte.jask.errors.compiler

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.Expression
import pt.iscte.jask.Language
import pt.iscte.jask.common.QuestionSequenceWithContext
import pt.iscte.jask.common.SimpleTextStatement
import pt.iscte.jask.errors.ICompilerError
import pt.iscte.jask.errors.compiler.templates.HowManyReturnStatements
import pt.iscte.jask.errors.compiler.templates.WhichReturnExecutesOnCondition
import pt.iscte.jask.extensions.ControlFlowGraph
import pt.iscte.jask.extensions.condition
import pt.iscte.jask.extensions.nameWithScope
import kotlin.math.min

class MissingReturnInBranch(
    val method: MethodDeclaration,
    private val flow: ControlFlowGraph,
    val branch: List<ControlFlowGraph.CFGEdge>
): ICompilerError {

    fun condition(): Expression =
        branch.condition()

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

    override fun toQLC(unit: CompilationUnit, language: Language): QuestionSequenceWithContext =
        QuestionSequenceWithContext(
            SimpleTextStatement(language["MissingReturn"].format(
                method.nameWithScope(),
                "return"
            )), listOfNotNull(
                HowManyReturnStatements(this).generate(unit, language),
                flow.findValidBranches().randomOrNull()?.let {
                    WhichReturnExecutesOnCondition(this, it).generate(unit, language)
                },
                WhichReturnExecutesOnCondition(this, branch).generate(unit, language)
            )
        )

    override fun message(): String = "Missing return statement:\n${pretty(branch)}"
}