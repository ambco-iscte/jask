package pt.iscte.pesca.compiler.errors

import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.types.ResolvedType
import pt.iscte.pesca.compiler.ICompilerError
import pt.iscte.pesca.extensions.success

class WrongTypeForVariableDeclaration(val variable: VariableDeclarator): ICompilerError {

    init {
        require(variable.initializer.isPresent) { "Variable must have an initialiser!" }
        require(success { variable.initializer.get().calculateResolvedType() }) {
            "Cannot resolve type of variable initialiser: ${variable.initializer.get()}"
        }
    }

    val expected: Type =
        variable.type

    val actual: ResolvedType =
        variable.initializer.get().calculateResolvedType()
    
    val initialiserIsMethodCall: Boolean
        get() = variable.initializer.get().isMethodCallExpr

    override fun message(): String = "Variable ${variable.nameAsString} is of type ${expected.asString()}, but is " +
            "initialised with expression of type ${actual.describe()}: ${variable.initializer.get()}"
}