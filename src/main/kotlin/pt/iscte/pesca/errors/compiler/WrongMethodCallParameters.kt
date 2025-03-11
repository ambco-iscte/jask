package pt.iscte.pesca.errors.compiler

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.types.ResolvedType
import pt.iscte.pesca.errors.ICompilerError
import pt.iscte.pesca.extensions.isValidFor
import pt.iscte.pesca.extensions.nameWithScope
import pt.iscte.pesca.extensions.success

data class WrongMethodCallParameters(val method: MethodDeclaration, val call: MethodCallExpr): ICompilerError {

    val expected: List<Type>
        get() = method.parameters.map { it.type }

    val actual: List<ResolvedType>
        get() = call.arguments.map { it.calculateResolvedType() }

    val parameterNumberMismatch: Boolean
        get() = expected.size != actual.size

    val parameterTypeMismatch: Boolean
        get() {
            call.arguments.forEachIndexed { i, arg ->
                runCatching {
                    val argumentType = arg.calculateResolvedType()
                    val parameterType = method.parameters[i].type.resolve()
                    if (!parameterType.isAssignableBy(argumentType))
                        return true
                }.getOrElse { return true }
            }
            return false
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