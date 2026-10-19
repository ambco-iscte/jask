package pt.iscte.jask.extensions

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.Parameter
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.EnclosedExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.nodeTypes.NodeWithStatements
import com.github.javaparser.ast.stmt.*
import java.util.*

data class ControlFlowGraph(
    private val method: MethodDeclaration
) {
    private val begin: VirtualStartNode = VirtualStartNode(
        UUID.randomUUID(),
        0,
        FunctionInputNode(
            UUID.randomUUID(),
            0,
            method.parameters,
            build(method.body.get().statements, 0)
        )
    )

    private val end = VirtualEndNode(UUID.randomUUID(), -1)

    init {
        require(method.body.isPresent)
    }

    private fun build(statements: List<Statement>, depth: Int, jumpToIfEmpty: CFGNode? = null): CFGNode {
        if (statements.isEmpty())
            return jumpToIfEmpty ?: EmptyNode(UUID.randomUUID(), depth, null)

        return when (val first: Statement = statements.first()) {
            is IfStmt -> {
                val next = build(statements.fromIndex(1), depth, jumpToIfEmpty)

                val then = if (first.hasThenBlock() && first.thenStmt.asBlockStmt().isEmpty) next
                else build(unwrap(first.thenStmt), depth + 1, next)

                val alternative: CFGNode? = first.elseStmt
                    .map { block: Statement ->
                        build(unwrap(block), depth + 1, next)
                    }.orElse(null)

                BranchNode(
                    UUID.randomUUID(),
                    depth,
                    next,
                    first,
                    first.condition,
                    then,
                    Optional.ofNullable(alternative),
                )
            }

            is WhileStmt -> {
                val next = build(statements.fromIndex(1), depth, jumpToIfEmpty)

                val loop = LoopNode(
                    UUID.randomUUID(),
                    depth,
                    next,
                    first,
                    first.condition
                )

                loop.body = if (first.hasEmptyBody()) EmptyNode(UUID.randomUUID(), depth, next)
                else build(unwrap(first.body), depth + 1, loop)

                loop
            }

            is BreakStmt -> {
                if (first.label.isPresent)
                    throw UnsupportedOperationException(
                        "Unsupported control flow for break statements with labels at: $first"
                    )
                BreakNode(UUID.randomUUID(), depth, first)
            }

            is ThrowStmt ->
                ExceptionNode(UUID.randomUUID(), depth, first, first.expression)

            is ReturnStmt ->
                ReturnNode(UUID.randomUUID(), depth, first, first.expression)

            else -> {
                val merge: MutableList<Statement> = mutableListOf(first)

                var i = 1
                while (i < statements.size) {
                    val statement = statements[i]
                    if (isSimpleStatement(statement))
                        merge.add(statement)
                    else break
                    i++
                }

                val tail: List<Statement> = statements.fromIndex(merge.size)

                SimpleNode(
                    UUID.randomUUID(),
                    depth,
                    merge,
                    build(tail, depth, jumpToIfEmpty)
                )
            }
        }
    }

    private fun nodes(edges: Set<CFGEdge>): Set<CFGNode> =
        edges.flatMap { listOf(it.from, it.to) }.toSet()

    private fun edges(): Set<CFGEdge> {
        val all = mutableSetOf<CFGEdge>()
        edges(null, begin, all, mutableSetOf())
        return all
    }

    private fun edges(
        containingLoop: LoopNode?,
        node: CFGNode,
        all: MutableSet<CFGEdge>,
        visited: MutableSet<CFGNode>
    ) {
        if (node in visited)
            return
        visited.add(node)

        when (node) {
            is VirtualStartNode -> {
                all.add(CFGEdge(node, node.next))
                edges(containingLoop, node.next, all, visited)
            }

            is FunctionInputNode -> {
                all.add(CFGEdge(node, node.next))
                edges(containingLoop, node.next, all, visited)
            }

            is SimpleNode -> {
                all.add(CFGEdge(node, node.next))
                edges(containingLoop, node.next, all, visited)
            }

            is BranchNode -> {
                all.add(CFGEdge(node, node.then, node.condition.equalsBoolean(true)))
                edges(containingLoop, node.then, all, visited)

                node.alternative.ifPresentOrElse({ alternative ->
                    all.add(CFGEdge(node, alternative, node.condition.equalsBoolean(false)))
                    edges(containingLoop, alternative, all, visited)
                }, {
                    all.add(CFGEdge(node, node.next, node.condition.equalsBoolean(false)))
                })

                edges(containingLoop, node.next, all, visited)
            }

            is LoopNode -> {
                all.add(CFGEdge(node, node.body, node.condition.equalsBoolean(true)))
                all.add(CFGEdge(node, node.next, node.condition.equalsBoolean(false)))

                edges(node, node.body, all, visited)
                edges(containingLoop, node.next, all, visited)
            }

            is BreakNode -> {
                if (containingLoop != null)
                    all.add(CFGEdge(node, containingLoop.next))
            }

            is ExceptionNode, is ReturnNode -> {
                all.add(CFGEdge(node, end))
            }

            is EmptyNode -> {
                if (containingLoop != null) {
                    all.add(CFGEdge(node, containingLoop))
                }
                else if (node.next != null) {
                    all.add(CFGEdge(node, node.next!!))
                    edges(containingLoop, node.next!!, all, visited)
                }
            }

            else -> {}
        }
    }

    fun existsPathTo(statement: Statement): Boolean =
        existsPathTo(begin, statement, mutableSetOf())

    private fun existsPathTo(first: CFGNode, statement: Statement, visited: MutableSet<CFGNode>): Boolean {
        if (first is SimpleNode && first.statements.contains(statement))
            return true

        if (first in visited)
            return false
        visited.add(first)

        return when (first) {
            is BranchNode ->
                existsPathTo(first.then, statement, visited) ||
                        first.alternative.map { existsPathTo(it, statement, visited) }.orElse(false)

            is LoopNode -> existsPathTo(first.body, statement, visited) || existsPathTo(first.next, statement, visited)

            else -> if (first.hasNext) existsPathTo(first.next!!, statement, visited) else false
        }
    }

    private fun existsPathBetween(first: CFGNode, second: CFGNode): Boolean {
        if (first == second)
            return true

        return when (first) {
            is BranchNode ->
                existsPathBetween(first.then, second) ||
                        first.alternative.map { existsPathBetween(it, second) }.orElse(false)

            is LoopNode -> existsPathBetween(first.body, second) || existsPathBetween(first.next, second)

            else -> if (first.hasNext) existsPathBetween(first.next!!, second) else false
        }
    }

    fun toDOTString(): String {
        val edges = edges()
        return """
        digraph %s {
            graph [nodesep=1, ranksep=0.1]
            edge [fontname="Consolas"]
            node [shape=rectangle, fontname="Consolas"]
        
            %s
        
            %s
        }
        """.trimIndent().format(
            method.nameAsString,
            nodes(edges).joinToString("\n\t") { node ->
                "\"${node.id}\"" + when (node) {
                    is EmptyNode ->
                        if (node.isTerminating)
                            " [shape=terminator, label=\"\\nUNDEFINED\", style=filled, fillcolor=red, fontcolor=red]"
                        else
                            " [shape=circle, label=\"\", width=.1, fixedsize=true]"

                    is VirtualStartNode ->
                        " [shape=circle, label=\"BEGIN\", style=filled, fillcolor=mediumseagreen, fontname=\"Palatino-Bold\"]"

                    is VirtualEndNode ->
                        " [shape=circle, label=\"END\", style=filled, fillcolor=gray, fontname=\"Palatino-Bold\"]"

                    is FunctionInputNode ->
                        " [shape=plaintext, label=\"${
                            if (node.input.isEmpty()) "START"
                            else "INPUT ${node.input.joinToString()}"
                        }\", style=filled, fillcolor=skyblue2]"

                    is ReturnNode ->
                        " [shape=plaintext, label=\"${
                            node.output.map { "OUTPUT ${it.escapeQuotes()}" }.orElse("HALT")
                        }\", style=filled, fillcolor=plum]"

                    is SimpleNode ->
                        " [label=\"${node.statements.joinToString("\\n") { it.escapeQuotes() }}\"]"

                    is BranchNode, is LoopNode ->
                        " [label=\"\", shape=diamond, fixedsize=true, width=.5, height=.5]"

                    is BreakNode ->
                        " [shape=plaintext, label=\"EXIT LOOP\"]"

                    is ExceptionNode ->
                        " [shape=plaintext, label=\"throw ${node.exception.escapeQuotes()}\", style=filled, fillcolor=tomato]"
                }
            },
            edges.joinToString("\n\t") { edge ->
                val label = edge.condition.map { " [label=\"${it.escapeQuotes()}\"]" }.orElse("")
                "\"${edge.from.id}\" -> \"${edge.to.id}\"$label"
            }
        )
    }

    private fun Node.escapeQuotes(): String =
        toString().replace("\"", "\\\"")

    sealed interface CFGNode {
        val id: UUID
        val depth: Int

        val next: CFGNode?
            get() = null

        val bind: Node?
            get() = null
    }

    private val CFGNode.hasNext: Boolean
        get() = next != null

    data class EmptyNode(
        override val id: UUID, override val depth: Int, override val next: CFGNode?
    ) : CFGNode {
        override fun toString(): String = "UNDEFINED"

        val isTerminating: Boolean
            get() = next == null
    }

    data class FunctionInputNode(
        override val id: UUID, override val depth: Int, val input: List<Parameter>, override val next: CFGNode
    ) : CFGNode {
        override fun toString(): String = "INPUT ${input.joinToString()}"
    }

    data class ReturnNode(
        override val id: UUID, override val depth: Int, override val bind: ReturnStmt, val output: Optional<Expression>
    ) : CFGNode {
        override fun toString(): String = output.map { "return $it" }.orElse("return")
    }

    data class ExceptionNode(
        override val id: UUID, override val depth: Int, override val bind: ThrowStmt, val exception: Expression
    ) : CFGNode {
        override fun toString(): String = "throw $exception"
    }

    data class BreakNode(
        override val id: UUID, override val depth: Int, override val bind: BreakStmt
    ) : CFGNode {
        override fun toString(): String = "break"
    }

    data class BranchNode(
        override val id: UUID, override val depth: Int, override val next: CFGNode,
        override val bind: IfStmt,
        val condition: Expression, val then: CFGNode, val alternative: Optional<CFGNode>
    ) : CFGNode {
        override fun toString(): String = "if ($condition)"
    }

    data class LoopNode(
        override val id: UUID, override val depth: Int, override val next: CFGNode, override val bind: WhileStmt,
        val condition: Expression
    ) : CFGNode {
        lateinit var body: CFGNode
            internal set

        override fun toString(): String = "while ($condition)"
    }

    data class SimpleNode(
        override val id: UUID, override val depth: Int, val statements: List<Statement>, override val next: CFGNode
    ) : CFGNode {
        constructor(id: UUID, depth: Int, statement: Statement, next: CFGNode) :
                this(id, depth, listOf(statement), next)

        val isEmpty: Boolean
            get() = statements.isEmpty() || statements.all { it.isEmptyStmt }

        override val bind: BlockStmt
            get() = BlockStmt(NodeList(statements))

        override fun toString(): String = statements.joinToString("; ")
    }

    private data class VirtualStartNode(
        override val id: UUID, override val depth: Int, override val next: CFGNode
    ) : CFGNode {
        override fun toString(): String = "BEGIN"
    }

    private data class VirtualEndNode(
        override val id: UUID, override val depth: Int
    ) : CFGNode {
        override fun toString(): String = "END"
    }

    data class CFGEdge(val from: CFGNode, val to: CFGNode, val condition: Optional<Expression>) {
        constructor(from: CFGNode, to: CFGNode, condition: Expression) :
                this(from, to, Optional.of(condition))

        constructor(from: CFGNode, to: CFGNode) : this(from, to, Optional.empty())
    }

    private fun unwrap(node: Statement): List<Statement> =
        if (node is NodeWithStatements<*>) node.statements else listOf(node)

    private fun isSimpleStatement(statement: Statement): Boolean =
        when (statement) {
            is IfStmt, is WhileStmt, is BreakStmt, is ThrowStmt, is ReturnStmt -> false
            else -> true
        }

    // TODO inefficient
    fun findBranchesWithMissingReturn(): List<List<CFGEdge>> {
        val all = edges()
        val endingInEmpty = all.filter { it.to is EmptyNode && it.to.isTerminating }

        if (endingInEmpty.isEmpty())
            return emptyList()

        val paths = mutableListOf<List<CFGEdge>>()

        endingInEmpty.forEach { final ->
            val path = mutableListOf(final)
            while (path.last().from != begin) {
                val previous = all.firstOrNull { it.to == path.last().from }
                if (previous == null)
                    break
                path.add(previous)
            }
            paths.add(path.reversed())
        }

        return paths
    }
}

fun main() {
    val source = """
        static boolean largestIsEven(int[] a) {
            if (a.length == 0) {
                return false;
            }
            
            int i = 1;
            int largest = a[0];
            
            while (i < a.length) {
                if (a[i] > largest) {
                    largest = a[i];
                }
                i = i + 1;
            }
            
            if (largest % 2 == 0) {
                return true;
            }
        }
        """.trimIndent()

    val foo = StaticJavaParser.parseMethodDeclaration(source)
    val flow = ControlFlowGraph(foo)
    println(flow.existsPathTo(ExpressionStmt(MethodCallExpr("NOPE"))))
    println()
    println(flow.toDOTString())
    println()
    flow.findBranchesWithMissingReturn().forEach { branch ->
        println(branch.joinToString(" ") { (from, to, condition) ->
            "$from ---${condition.map { "($it)" }.orElse("")}--->${if (to is ControlFlowGraph.EmptyNode) " $to"   else ""}"
        })
        println()
    }
}