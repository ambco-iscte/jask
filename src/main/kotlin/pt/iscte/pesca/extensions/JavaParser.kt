package pt.iscte.pesca.extensions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.nodeTypes.NodeWithBody
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName
import com.github.javaparser.ast.stmt.DoStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.WhileStmt
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.type.Type
import pt.iscte.strudel.parsing.java.extensions.getOrNull
import java.util.Locale
import java.util.Optional

inline fun <reified T : Node> find(source: String, condition: (T) -> Boolean = { true }): List<T> =
    StaticJavaParser.parse(source).findAll(T::class.java).filter { condition(it) }

inline fun <reified T : Node> Node.findAll(noinline condition: (T) -> Boolean = { true }): List<T> =
    findAll(T::class.java, condition)

val MethodDeclaration.prettySignature: String
    get() = "$typeAsString $nameAsString(${parameters.joinToString()})"

fun Node.hasMethodCalls(): Boolean =
    findAll(MethodCallExpr::class.java).isNotEmpty()

fun Node.getLoopControlStructures(): List<NodeWithBody<*>> =
    findAll(ForStmt::class.java) +
    findAll(DoStmt::class.java) +
    findAll(WhileStmt::class.java) +
    findAll(ForEachStmt::class.java)

fun Node.hasLoopControlStructures(): Boolean =
    getLoopControlStructures().isNotEmpty()

val JAVA_PRIMITIVE_TYPES: Set<String> =
    PrimitiveType.Primitive.values().map { it.name.lowercase(Locale.getDefault()) }.toSet()

fun MethodDeclaration.returnsPrimitiveOrArrayOrString(): Boolean =
    type.isPrimitiveType || type.isArrayType || type.toString() == String::class.simpleName

fun MethodDeclaration.getUsedTypes(): List<Type> =
    listOf(type) +
    parameters.map { it.type } +
    findAll(VariableDeclarationExpr::class.java).flatMap { it.variables.map { it.type } }

// TODO probably super weak
fun MethodDeclaration.getReturnVariables(): Map<ReturnStmt, List<NodeWithSimpleName<*>>> =
    findAll(ReturnStmt::class.java).associateWith { ret ->
        ret.findAll(Node::class.java).filterIsInstance<NodeWithSimpleName<*>>()
    }

fun MethodDeclaration.getLocalVariables(): List<VariableDeclarator> =
    findAll(VariableDeclarationExpr::class.java).flatMap { it.variables }

fun MethodDeclaration.getUsableVariables(): List<VariableDeclarator> =
    getLocalVariables() + (findAncestor(TypeDeclaration::class.java).getOrNull?.findAll(FieldDeclaration::class.java)?.flatMap {
        it.variables
    } ?: listOf())

fun MethodCallExpr.asString(): String =
    (if (scope.isPresent) "${scope.get()}." else "") + nameAsString

fun Node.getVariablesInScope(): Set<String> {
    val variables = mutableSetOf<String>()

    val start: Node = this
    val line = start.range.get().begin.line

    var current: Optional<Node> = Optional.of(this)
    while (current.isPresent) {
        val node = current.get()

        if (node.range.isPresent) {
            variables.addAll(node.findAll<VariableDeclarator>().filter {
                it.range.isPresent && it.range.get().end.line < line
            }.map { it.nameAsString })
        }
        if (node is MethodDeclaration)
            variables.addAll(node.asMethodDeclaration().parameters.map { it.nameAsString })

        current = node.parentNode
    }

    return variables
}