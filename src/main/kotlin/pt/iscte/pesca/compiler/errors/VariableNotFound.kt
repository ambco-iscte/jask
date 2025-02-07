package pt.iscte.pesca.compiler.errors

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.NameExpr
import pt.iscte.pesca.compiler.JavacErrorType
import pt.iscte.pesca.compiler.ICompilerError
import pt.iscte.strudel.parsing.java.extensions.nodeAtLine
import javax.tools.Diagnostic
import javax.tools.JavaFileObject

data class VariableNotFound(val symbol: String, val location: Node): ICompilerError {

    companion object {
        fun of(source: CompilationUnit, diagnostic: Diagnostic<out JavaFileObject>): VariableNotFound {
            require(JavacErrorType.get(diagnostic) == JavacErrorType.CannotResolveLocation)

            val node = source.nodeAtLine(diagnostic.lineNumber.toInt())!!

            val variable = node.findAll(NameExpr::class.java)?.firstOrNull {
                " ${it.nameAsString}\n" in diagnostic.getMessage(null)
            }?.nameAsString

            return VariableNotFound(variable ?: node.toString(), node)
        }
    }
}