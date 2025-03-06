package pt.iscte.pesca.compiler.errors

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import pt.iscte.pesca.compiler.ICompilerError
import pt.iscte.pesca.extensions.nameWithScope

data class UnknownMethod(val call: MethodCallExpr, val usable: List<MethodDeclaration>): ICompilerError {

    // TODO: what if student meant to call a Java native method instead of a method they implemented

    override fun message(): String =
        "Unknown method ${call.nameAsString} in: $call\n\tUsable declared methods are: ${usable.joinToString { it.nameWithScope() }}"
}