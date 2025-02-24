package pt.iscte.pesca.compiler.errors

import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.types.ResolvedType
import pt.iscte.pesca.compiler.ICompilerError

class IncompatibleVariableType(val variable: VariableDeclarator): ICompilerError {

    val expected: Type =
        variable.type

    val actual: ResolvedType =
        variable.initializer.get().calculateResolvedType()

    override fun message(): String = "Variable ${variable.nameAsString} is of type ${expected.asString()}, but is " +
            "initialised with expression of type ${actual.describe()}: ${variable.initializer.get()}"
}