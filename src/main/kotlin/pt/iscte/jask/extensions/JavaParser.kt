package pt.iscte.jask.extensions

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.Position
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.nodeTypes.NodeWithBody
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.PrimitiveType
import com.github.javaparser.ast.type.Type
import com.github.javaparser.ast.visitor.GenericListVisitorAdapter
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import pt.iscte.strudel.parsing.java.extensions.getOrNull
import java.util.Locale
import java.util.Optional
import kotlin.math.abs

fun configureStaticJavaParser() {
    StaticJavaParser.getParserConfiguration().languageLevel = ParserConfiguration.LanguageLevel.JAVA_20
    StaticJavaParser.getParserConfiguration().setSymbolResolver(
        JavaSymbolSolver(CombinedTypeSolver().apply { add(ReflectionTypeSolver()) })
    )
}

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
    getLocalVariables() + (findAncestor(TypeDeclaration::class.java).getOrNull?.findAll(FieldDeclaration::class.java)
        ?.flatMap {
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

    val ifStatements = if (thenStmt.isBlockStmt) {
        thenStmt.asBlockStmt().statements
    } else {
        listOf(thenStmt)  // Wrap single statement in a list
    }

    val elseStatements = if (elseStmt.get().isBlockStmt) {
        elseStmt.get().asBlockStmt().statements
    } else {
        listOf(elseStmt.get())  // Wrap single statement in a list
    }

    return elseStatements == ifStatements

}

fun MethodDeclaration.hasDuplicatedIfElse(): Boolean =
    this.findAll(IfStmt::class.java).any { ifStmt ->
        ifStmt.hasDuplicateCode()
    }

fun MethodDeclaration.hasDuplicatedInsideIfElse(): IfStmt? =
    this.findAll(IfStmt::class.java).firstOrNull() { ifStmt ->
        val (prefix, suffix) = getCommonStatements(ifStmt)
        (prefix.isNotEmpty() || suffix.isNotEmpty()) && prefix != suffix
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
        is BooleanLiteralExpr -> {
            BooleanLiteralExpr(!expression.value)
        }


        else -> UnaryExpr(expression, UnaryExpr.Operator.LOGICAL_COMPLEMENT) // Default case
    }
}


fun removeEqualsTrueOrFalse(expression: Expression): Expression {
    when (expression) {
        is BinaryExpr -> {
            when (expression.operator) {
                BinaryExpr.Operator.AND -> return BinaryExpr(
                    removeEqualsTrueOrFalse(expression.left),
                    removeEqualsTrueOrFalse(expression.right),
                    BinaryExpr.Operator.AND
                )

                BinaryExpr.Operator.OR -> return BinaryExpr(
                    removeEqualsTrueOrFalse(expression.left),
                    removeEqualsTrueOrFalse(expression.right),
                    BinaryExpr.Operator.OR
                )

                BinaryExpr.Operator.EQUALS -> { // Handle '=='
                    when {
                        expression.right is BooleanLiteralExpr && (expression.right as BooleanLiteralExpr).value -> {
                            return expression.left // Replace (a == true) with a
                        }

                        expression.left is BooleanLiteralExpr && (expression.left as BooleanLiteralExpr).value -> {
                            return expression.right // Replace (true == a) with a
                        }

                        expression.right is BooleanLiteralExpr && !(expression.right as BooleanLiteralExpr).value -> {
                            return UnaryExpr(
                                expression.left,
                                UnaryExpr.Operator.LOGICAL_COMPLEMENT
                            ) // Replace (a == false) with !a
                        }

                        expression.left is BooleanLiteralExpr && !(expression.left as BooleanLiteralExpr).value -> {
                            return UnaryExpr(
                                expression.right,
                                UnaryExpr.Operator.LOGICAL_COMPLEMENT
                            ) // Replace (false == a) with !a
                        }
                    }
                }

                BinaryExpr.Operator.NOT_EQUALS -> { // Handle '!='
                    when {
                        expression.right is BooleanLiteralExpr && (expression.right as BooleanLiteralExpr).value -> {
                            return UnaryExpr(
                                expression.left,
                                UnaryExpr.Operator.LOGICAL_COMPLEMENT
                            ) // Replace (a != true) with !a
                        }

                        expression.left is BooleanLiteralExpr && (expression.left as BooleanLiteralExpr).value -> {
                            return UnaryExpr(
                                expression.right,
                                UnaryExpr.Operator.LOGICAL_COMPLEMENT
                            ) // Replace (true != a) with !a
                        }

                        expression.right is BooleanLiteralExpr && !(expression.right as BooleanLiteralExpr).value -> {
                            return expression.left // Replace (a != false) with a
                        }

                        expression.left is BooleanLiteralExpr && !(expression.left as BooleanLiteralExpr).value -> {
                            return expression.right // Replace (false != a) with a
                        }
                    }
                }

                else -> {}
            }
        }
    }
    return expression
}


fun findUselessVariableDeclarations(element: MethodDeclaration): List<Pair<Statement,Statement>> {
    fun checkBlock(statements: List<Statement>): List<Pair<Statement,Statement>> {
        val declaredVariables = mutableMapOf<String, Int>() // Stores variable names and their declaration index
        val lastAssignedStatement = mutableMapOf<String, Int>() // Tracks last assigned statement index
        val unnecessaryDeclarations = mutableListOf<Pair<Statement,Statement>>()

        for (i in statements.indices) {
            val currentStmt = statements[i]

            // Check if the statement is a variable declaration
            if (currentStmt is ExpressionStmt && currentStmt.expression is VariableDeclarationExpr) {
                val varDecl = currentStmt.expression as VariableDeclarationExpr

                if (varDecl.variables.size == 1) {
                    val variableName = varDecl.variables[0].nameAsString
                    declaredVariables[variableName] = i
                }
            }

            // Check if the statement is an assignment
            if (currentStmt is ExpressionStmt && currentStmt.expression is AssignExpr) {
                val assignExpr = currentStmt.expression as AssignExpr
                val assignedVar = assignExpr.target

                if (assignedVar is NameExpr) {
                    val variableName = assignedVar.nameAsString

                    // Check if the variable was previously declared but not used before assignment
                    val declarationIndex = declaredVariables[variableName]
                    if (declarationIndex != null) {
                        val isUnusedBeforeAssignment = (declarationIndex + 1 until i).all { stmtIndex ->
                            val stmt = statements[stmtIndex]
                            !stmt.findAll(NameExpr::class.java).any { it.nameAsString == variableName }
                        }
                        if (isUnusedBeforeAssignment) {
                            unnecessaryDeclarations.add(Pair(statements[declarationIndex], currentStmt))
                        }
                    }

                    // Check if the variable was assigned before but not used in between
                    val lastAssignmentIndex = lastAssignedStatement[variableName]
                    if (lastAssignmentIndex != null) {
                        val isUnusedBetweenAssignments = (lastAssignmentIndex + 1 until i).all { stmtIndex ->
                            val stmt = statements[stmtIndex]
                            !stmt.findAll(NameExpr::class.java).any { it.nameAsString == variableName }
                        }
                        if (isUnusedBetweenAssignments) {
                            unnecessaryDeclarations.add(Pair(statements[lastAssignmentIndex], currentStmt))
                        }
                    }

                    // Update last assigned statement index
                    lastAssignedStatement[variableName] = i
                }
            }

            // Recursively check nested blocks (if, while, for, etc.)
            fun extractStatements(stmt: Statement?): List<Statement> {
                return when (stmt) {
                    is BlockStmt -> stmt.statements
                    null -> emptyList()
                    else -> listOf(stmt) // Treat single-line statements as a "block"
                }
            }

            if (currentStmt is IfStmt) {
                unnecessaryDeclarations.addAll(checkBlock(extractStatements(currentStmt.thenStmt)))
                unnecessaryDeclarations.addAll(checkBlock(extractStatements(currentStmt.elseStmt.orElse(null))))
            } else if (currentStmt is WhileStmt) {
                unnecessaryDeclarations.addAll(checkBlock(extractStatements(currentStmt.body)))
            } else if (currentStmt is ForStmt) {
                unnecessaryDeclarations.addAll(checkBlock(extractStatements(currentStmt.body)))
            } else if (currentStmt is ForEachStmt) {
                unnecessaryDeclarations.addAll(checkBlock(extractStatements(currentStmt.body)))
            }
        }
        return unnecessaryDeclarations
    }

    return checkBlock(element.body.orElse(null)?.statements ?: emptyList())
}

fun mergeVariableDeclaration(pair: Pair<Statement, Statement>): Statement? {
    val (declaration, assignment) = pair

    if (declaration is ExpressionStmt && assignment is ExpressionStmt) {
        val declExpr = declaration.expression
        val assignExpr = assignment.expression

        if (declExpr is VariableDeclarationExpr && assignExpr is AssignExpr) {
            if (declExpr.variables.size == 1 && assignExpr.target is NameExpr) {
                val variable = declExpr.variables[0]
                val assignedValue = assignExpr.value

                if (variable.nameAsString == (assignExpr.target as NameExpr).nameAsString) {
                    // Create a merged variable declaration with initialization
                    val mergedDeclaration = VariableDeclarationExpr(variable.type, variable.nameAsString)
                    mergedDeclaration.variables[0].setInitializer(assignedValue)
                    return ExpressionStmt(mergedDeclaration)
                }
            }
        } else if (declExpr is AssignExpr && assignExpr is AssignExpr) {
            val declTarget = declExpr.target
            val assignTarget = assignExpr.target

            if (declTarget is NameExpr && assignTarget is NameExpr &&
                declTarget.nameAsString == assignTarget.nameAsString) {

                // Return only the second assignment, effectively removing the first one
                return ExpressionStmt(AssignExpr(NameExpr(assignTarget.nameAsString), assignExpr.value, AssignExpr.Operator.ASSIGN))
            }
        }
    }
    return null
}
// Function to check if an expression contains multiple comparisons
fun hasMultipleComparisons(expr: Expression): Boolean {
    if (expr is BinaryExpr) {
        val leftIsComparison = isComparison(expr.left)
        val rightIsComparison = isComparison(expr.right)
        return leftIsComparison || rightIsComparison // If either side is a comparison, check further
    }
    return false
}

// Helper function to check if an expression is a comparison (>, <, >=, <=, ==, !=)
fun isComparison(expr: Expression): Boolean {
    return expr is BinaryExpr && expr.operator in listOf(
        BinaryExpr.Operator.GREATER,
        BinaryExpr.Operator.LESS,
        BinaryExpr.Operator.GREATER_EQUALS,
        BinaryExpr.Operator.LESS_EQUALS,
        BinaryExpr.Operator.EQUALS,
        BinaryExpr.Operator.NOT_EQUALS
    )
}

// Function to add parentheses only when needed
fun wrapIfNeeded(expr: Expression): Expression {
    return if (hasMultipleComparisons(expr)) EnclosedExpr(expr) else expr
}

fun extractReturnExpr(stmt: Statement): Expression? = when {
    stmt.isReturnStmt ->
        stmt.asReturnStmt().expression.orElse(null)

    stmt.isBlockStmt ->                     // bloco { ... }
        stmt.asBlockStmt()
            .statements
            .firstOrNull { it.isReturnStmt } // pega o primeiro return do bloco
            ?.asReturnStmt()
            ?.expression
            ?.orElse(null)

    else -> null
}

fun findReturnWithDeadCode(node: Node): ReturnStmt? {
    when (node) {
        is BlockStmt -> {
            val statements = node.statements
            for (i in statements.indices) {
                val stmt = statements[i]
                if (stmt is ReturnStmt && i < statements.size - 1) {
                    return stmt
                }
                val found = findReturnWithDeadCode(stmt)
                if (found != null) return found
            }
        }

        is IfStmt -> {
            node.thenStmt.let {
                val found = findReturnWithDeadCode(it)
                if (found != null) return found
            }
            node.elseStmt.orElse(null)?.let {
                val found = findReturnWithDeadCode(it)
                if (found != null) return found
            }
        }

        is WhileStmt -> {
            node.body.let {
                val found = findReturnWithDeadCode(it)
                if (found != null) return found
            }
        }

        is ForStmt -> {
            node.body.let {
                val found = findReturnWithDeadCode(it)
                if (found != null) return found
            }
        }

        is ForEachStmt -> {
            node.body.let {
                val found = findReturnWithDeadCode(it)
                if (found != null) return found
            }
        }

        is DoStmt -> {
            node.body.let {
                val found = findReturnWithDeadCode(it)
                if (found != null) return found
            }
        }

        is TryStmt -> {
            val foundTry = findReturnWithDeadCode(node.tryBlock)
            if (foundTry != null) return foundTry

            for (catchClause in node.catchClauses) {
                val foundCatch = findReturnWithDeadCode(catchClause.body)
                if (foundCatch != null) return foundCatch
            }

            if (node.finallyBlock.isPresent) {
                val foundFinally = findReturnWithDeadCode(node.finallyBlock.get())
                if (foundFinally != null) return foundFinally
            }
        }
    }

    return null
}

fun findLonelyVariables(method: MethodDeclaration): List<String> {
    val body = method.body.orElse(null) ?: return emptyList()

    // Get all declared variable names
    val declaredVars = body.findAll(VariableDeclarationExpr::class.java)
        .flatMap { it.variables }
        .map { it.nameAsString }

    // Track names that are actually READ
    val readVars = mutableSetOf<String>()

    body.walk(NameExpr::class.java) { nameExpr ->
        val parent = nameExpr.parentNode.orElse(null)
        val name = nameExpr.nameAsString

        val isRead = when (parent) {
            is AssignExpr -> parent.target != nameExpr // it's on the right-hand side (i.e. read)
            is UnaryExpr -> true // ++a or a++ might be read
            else -> true
        }

        if (isRead) {
            readVars.add(name)
        }
    }

    return declaredVars.filter { it !in readVars }
}

fun findStatementsUsingVariables(root: Node, variableNames: List<String>): List<Statement> {
    val result = mutableSetOf<Statement>()

    // Find variable declarations involving lonely variables
    root.findAll(VariableDeclarationExpr::class.java).forEach { declExpr ->
        declExpr.variables.forEach { varDecl ->
            if (varDecl.nameAsString in variableNames) {
                val stmt = declExpr.findAncestor(Statement::class.java).orElse(null)
                if (stmt != null) result.add(stmt)
            }
        }
    }

    // Find other statements using the variable names (e.g., assignments)
    root.findAll(NameExpr::class.java).forEach { nameExpr ->
        if (nameExpr.nameAsString in variableNames) {
            val stmt = nameExpr.findAncestor(Statement::class.java).orElse(null)
            if (stmt != null) result.add(stmt)
        }
    }

    return result.toList()
}
fun replaceIfWithThenBody(ifStmt: IfStmt) {
    val parent = ifStmt.parentNode.orElse(null) ?: return

    val thenStmt = ifStmt.thenStmt
    val replacementStmts = when (thenStmt) {
        is BlockStmt -> thenStmt.statements
        else -> listOf(thenStmt)
    }

    when (parent) {
        is BlockStmt -> {
            val stmts = parent.statements
            val index = stmts.indexOf(ifStmt)
            if (index >= 0) {
                stmts.removeAt(index)
                stmts.addAll(index, replacementStmts)
            }
        }

        is Statement -> {
            // Single-statement context: wrap in BlockStmt
            val block = BlockStmt()
            block.statements.addAll(replacementStmts)
            ifStmt.replace(block)
        }

        else -> {
            // General fallback: try wrapping in block
            val block = BlockStmt()
            block.statements.addAll(replacementStmts)
            ifStmt.replace(block)
        }
    }
}

fun getCommonStatements(ifStmt: IfStmt): Pair<List<Statement>, List<Statement>> {
    if (!ifStmt.elseStmt.isPresent) return emptyList<Statement>() to emptyList()

    val thenStatements = if (ifStmt.thenStmt.isBlockStmt) {
        ifStmt.thenStmt.asBlockStmt().statements
    } else {
        listOf(ifStmt.thenStmt)
    }

    val elseStatements = if (ifStmt.elseStmt.get().isBlockStmt) {
        ifStmt.elseStmt.get().asBlockStmt().statements
    } else {
        listOf(ifStmt.elseStmt.get())
    }

    // Common prefix
    val prefix = mutableListOf<Statement>()
    for (i in 0 until minOf(thenStatements.size, elseStatements.size)) {
        if (thenStatements[i] == elseStatements[i]) {
            prefix.add(thenStatements[i])
        } else break
    }

    // Common suffix
    val suffix = mutableListOf<Statement>()
    for (i in 1..minOf(thenStatements.size, elseStatements.size)) {
        if (thenStatements[thenStatements.size - i] == elseStatements[elseStatements.size - i]) {
            suffix.add(0, thenStatements[thenStatements.size - i])
        } else break
    }

    return prefix to suffix
}

fun refactorIfByExtractingCommonParts(ifStmt: IfStmt): IfStmt {
    val (prefix, suffix) = getCommonStatements(ifStmt)
    if (prefix.isEmpty() && suffix.isEmpty()) return ifStmt

    val thenStatements = if (ifStmt.thenStmt.isBlockStmt) {
        ifStmt.thenStmt.asBlockStmt().statements
    } else listOf(ifStmt.thenStmt)

    val elseStatements = if (ifStmt.elseStmt.get().isBlockStmt) {
        ifStmt.elseStmt.get().asBlockStmt().statements
    } else listOf(ifStmt.elseStmt.get())

    val trimmedThen = thenStatements.subList(
        prefix.size, thenStatements.size - suffix.size
    )
    val trimmedElse = elseStatements.subList(
        prefix.size, elseStatements.size - suffix.size
    )

    val newThenBlock = BlockStmt().apply { trimmedThen.forEach { addStatement(it.clone()) } }
    val newElseBlock = BlockStmt().apply { trimmedElse.forEach { addStatement(it.clone()) } }
    val newIf = IfStmt(ifStmt.condition.clone(), newThenBlock, newElseBlock)

    val parent = ifStmt.parentNode.orElse(null)
    if (parent is BlockStmt) {
        val idx = parent.statements.indexOf(ifStmt)
        parent.statements.removeAt(idx)
        parent.statements.addAll(idx, prefix.map { it.clone() })
        parent.statements.add(idx + prefix.size, newIf)
        parent.statements.addAll(idx + prefix.size + 1, suffix.map { it.clone() })
    } else {
        val block = BlockStmt()
        prefix.forEach { block.addStatement(it.clone()) }
        block.addStatement(newIf)
        suffix.forEach { block.addStatement(it.clone()) }
        ifStmt.replace(block)
    }

    return newIf
}

fun getSelfAssignments(methodDeclaration: MethodDeclaration): List<Statement> {
    return methodDeclaration.accept(object : GenericListVisitorAdapter<Statement, Void?>() {
        override fun visit(n: ExpressionStmt, arg: Void?): List<Statement> {
            val expr = n.expression
            if (expr is AssignExpr) {
                val target = expr.target
                val value = expr.value

                if (target is NameExpr && value is NameExpr) {
                    if (target.nameAsString == value.nameAsString) {
                        return listOf(n)
                    }
                }
            }
            return emptyList()
        }
    }, null)
}