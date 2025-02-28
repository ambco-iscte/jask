package pt.iscte.pesca.compiler.errors

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.types.ResolvedType
import pt.iscte.pesca.compiler.ICompilerError

data class IncompatibleReturnType(val method: MethodDeclaration, val returnStmt: ReturnStmt): ICompilerError {

    val expected: Type =
        method.type

    val actual: ResolvedType =
        returnStmt.expression.get().calculateResolvedType()

    override fun message(): String = "Incompatible return type ${actual.describe()} in: $returnStmt\n\t" +
            "Method ${method.nameAsString} should return ${method.typeAsString}"
}