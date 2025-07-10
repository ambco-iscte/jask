package pt.iscte.jask.errors.compiler

import com.github.javaparser.ast.expr.NameExpr
import pt.iscte.jask.errors.ICompilerError
import pt.iscte.jask.errors.VariableScoping
import pt.iscte.strudel.parsing.java.extensions.getOrNull

data class UnknownVariable(val expr: NameExpr, val scope: VariableScoping.Scope<*>): ICompilerError {

    override fun message(): String =
        "Unknown variable $expr in: ${expr.parentNode.getOrNull ?: expr}"
}