package pt.iscte.pesca.extensions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.nodeTypes.NodeWithBody
import com.github.javaparser.ast.stmt.DoStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.WhileStmt
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.type.Type
import pt.iscte.pesca.questions.SourceCode
import java.util.Locale

inline fun <reified T : Node> find(source: String, condition: (T) -> Boolean = { true }): List<T> =
    StaticJavaParser.parse(source).findAll(T::class.java).filter { condition(it) }

inline fun <reified T : Node> Node.findAll(noinline condition: (T) -> Boolean = { true }) =
    findAll(T::class.java, condition)

fun MethodDeclaration.nameMatches(name: String?): Boolean =
    if (name == null) true
    else nameAsString == name

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