package pt.iscte.pesca.errors.compiler

import com.github.javaparser.ast.expr.NameExpr
import pt.iscte.pesca.errors.ICompilerError
import pt.iscte.pesca.errors.VariableScoping
import pt.iscte.strudel.parsing.java.extensions.getOrNull

data class UnknownVariable(val expr: NameExpr, val scope: VariableScoping.Scope<*>): ICompilerError {

    override fun message(): String =
        "Unknown variable $expr in: ${expr.parentNode.getOrNull ?: expr}\n\tVariables in scope: ${scope.getUsableVariables().joinToString()}"
}