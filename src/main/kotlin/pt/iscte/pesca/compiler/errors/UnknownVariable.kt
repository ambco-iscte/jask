package pt.iscte.pesca.compiler.errors

import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.Statement
import pt.iscte.pesca.compiler.ICompilerError
import pt.iscte.pesca.compiler.VariableScoping
import pt.iscte.strudel.parsing.java.extensions.getOrNull

data class UnknownVariable(val expr: NameExpr, val scope: VariableScoping.Scope<*>): ICompilerError {

    override fun message(): String =
        "Unknown variable $expr in: ${expr.parentNode.getOrNull ?: expr}\n\tVariables in scope: ${scope.getUsableVariables().joinToString()}"
}