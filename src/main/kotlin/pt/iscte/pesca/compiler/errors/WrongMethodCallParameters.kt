package pt.iscte.pesca.compiler.errors

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration
import com.github.javaparser.resolution.types.ResolvedType
import pt.iscte.pesca.compiler.ICompilerError
import pt.iscte.pesca.extensions.isValidFor
import pt.iscte.pesca.extensions.nameWithScope
import pt.iscte.pesca.extensions.success

data class WrongMethodCallParameters(val method: MethodDeclaration, val call: MethodCallExpr): ICompilerError {

    val expected: List<Type> =
        method.parameters.map { it.type }

    val actual: List<ResolvedType> =
        call.arguments.map { it.calculateResolvedType() }

    val parameterNumberMismatch: Boolean
        get() = expected.size != actual.size

    val parameterTypeMismatch: Boolean
        get() {
            call.arguments.forEachIndexed { i, arg ->
                runCatching {
                    val argumentType = arg.calculateResolvedType()
                    val parameterType = method.parameters[i].type.resolve()
                    parameterType.isAssignableBy(argumentType)
                }.getOrElse { return false }
            }
            return true
        }

    init {
        call.arguments.forEach {
            require(success { it.calculateResolvedType() }) {
                "Cannot resolve type of argument $it in: $call"
            }
        }
        require(!call.isValidFor(method)) { "Method call $call must be invalid for method ${method.nameWithScope()}!" }
    }

    override fun message(): String = ""
}