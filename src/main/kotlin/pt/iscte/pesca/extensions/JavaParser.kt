package pt.iscte.pesca.extensions

import com.github.javaparser.Position
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.FieldAccessExpr
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.nodeTypes.NodeWithBody
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.type.Type
import pt.iscte.strudel.parsing.java.extensions.getOrNull
import java.util.Locale
import java.util.Optional
import kotlin.math.abs

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

fun MethodCallExpr.nameWithScope(): String =
    (if (scope.isPresent) "${scope.get()}." else "") + nameAsString

fun MethodDeclaration.nameWithScope(): String {
    val type = findAncestor(TypeDeclaration::class.java)
    return if (type.isPresent)
        "${type.get().nameAsString}.${nameAsString}"
    else
        nameAsString
}

fun MethodCallExpr.findMethodDeclaration(): Optional<MethodDeclaration> {
    val unit = findCompilationUnit()
    if (unit.isEmpty)
        return Optional.empty<MethodDeclaration>()

    return Optional.ofNullable(unit.get().findAll(MethodDeclaration::class.java).filter {
        if (this.scope.isPresent)
            it.nameWithScope() == this.nameWithScope()
        else
            it.nameAsString == this.nameAsString
    }.minByOrNull { abs(it.parameters.size - this.arguments.size) })
}

fun MethodCallExpr.isValidFor(method: MethodDeclaration): Boolean {
    if (arguments.size != method.parameters.size)
        return false
    arguments.forEachIndexed { i, arg ->
        runCatching {
            val argumentType = arg.calculateResolvedType()
            val parameterType = method.parameters[i].type.resolve()
            if (!parameterType.isAssignableBy(argumentType))
                return false
        }.getOrElse { return false }
    }
    return true
}

fun Position.relativeTo(other: Position): Position =
    Position(line - other.line + 1, column - other.column + 1)

fun IfStmt.hasDuplicateCode(): Boolean {
    if (!elseStmt.isPresent) return false

    val IfStatements = if (thenStmt.isBlockStmt) {
        thenStmt.asBlockStmt().statements
    } else {
        listOf(thenStmt)  // Wrap single statement in a list
    }

    val ElseStatements = if (elseStmt.get().isBlockStmt) {
        elseStmt.get().asBlockStmt().statements
    } else {
        listOf(elseStmt.get())  // Wrap single statement in a list
    }

    return ElseStatements == IfStatements

}

fun MethodDeclaration.hasDuplicatedIfElse(): Boolean{
    this.findAll(IfStmt::class.java).forEach { ifStmt ->
        if (ifStmt.hasDuplicateCode()) {
                return true
        }
    }
    return false
}

fun Node.lineRelativeTo(other: Node): Int =
    range.get().begin.relativeTo(other.range.get().begin).line



fun negateExpression(expression: Expression): Expression {
    return when (expression) {
        is BinaryExpr -> {
            when (expression.operator) {
                BinaryExpr.Operator.AND -> BinaryExpr(
                    negateExpression(expression.left),
                    negateExpression(expression.right),
                    BinaryExpr.Operator.OR
                )
                BinaryExpr.Operator.OR -> BinaryExpr(
                    negateExpression(expression.left),
                    negateExpression(expression.right),
                    BinaryExpr.Operator.AND
                )
                BinaryExpr.Operator.EQUALS -> BinaryExpr(
                    expression.left, expression.right, BinaryExpr.Operator.NOT_EQUALS
                )
                BinaryExpr.Operator.NOT_EQUALS -> BinaryExpr(
                    expression.left, expression.right, BinaryExpr.Operator.EQUALS
                )
                BinaryExpr.Operator.GREATER -> BinaryExpr(
                    expression.left, expression.right, BinaryExpr.Operator.LESS_EQUALS
                )
                BinaryExpr.Operator.GREATER_EQUALS -> BinaryExpr(
                    expression.left, expression.right, BinaryExpr.Operator.LESS
                )
                BinaryExpr.Operator.LESS -> BinaryExpr(
                    expression.left, expression.right, BinaryExpr.Operator.GREATER_EQUALS
                )
                BinaryExpr.Operator.LESS_EQUALS -> BinaryExpr(
                    expression.left, expression.right, BinaryExpr.Operator.GREATER
                )
                else -> UnaryExpr(expression, UnaryExpr.Operator.LOGICAL_COMPLEMENT) // Default case
            }
        }
        is UnaryExpr -> {
            if (expression.operator == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
                expression.expression // Remove double negation (!!A → A)
            } else {
                UnaryExpr(expression, UnaryExpr.Operator.LOGICAL_COMPLEMENT)
            }
        }
        else -> UnaryExpr(expression, UnaryExpr.Operator.LOGICAL_COMPLEMENT) // Default case
    }
}