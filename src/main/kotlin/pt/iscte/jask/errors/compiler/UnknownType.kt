package pt.iscte.jask.errors.compiler

import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.type.Type
import pt.iscte.jask.errors.ICompilerError

data class UnknownType(val type: Type, val location: Node, val types: List<TypeDeclaration<*>>): ICompilerError {

    override fun message(): String = "Unknown type ${type.asString()} in: $location\n\t" +
            "Usable declared types are: ${types.joinToString { it.nameAsString }}"
}