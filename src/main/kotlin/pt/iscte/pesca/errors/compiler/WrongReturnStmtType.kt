package pt.iscte.pesca.errors.compiler

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.types.ResolvedType
import pt.iscte.pesca.errors.ICompilerError
import pt.iscte.pesca.extensions.success

data class WrongReturnStmtType(val method: MethodDeclaration, val returnStmt: ReturnStmt): ICompilerError {

    init {
        require(returnStmt.expression.isPresent) { "Return statement must return an expression!" }
        require(success { returnStmt.expression.get().calculateResolvedType() }) {
            "Cannot resolve type of return expression: ${returnStmt.expression.get()}"
        }
    }

    val expected: Type =
        method.type

    val actual: ResolvedType =
        returnStmt.expression.get().calculateResolvedType()

    override fun message(): String = "Incompatible return type ${actual.describe()} in: $returnStmt\n\t" +
            "Method ${method.nameAsString} should return ${method.typeAsString}"
}