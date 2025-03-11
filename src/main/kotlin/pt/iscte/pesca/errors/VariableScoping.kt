package pt.iscte.pesca.errors

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration
import com.github.javaparser.ast.body.CompactConstructorDeclaration
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.RecordDeclaration
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import java.util.Optional

object VariableScoping {

    interface ScopeVisitor {
        fun visit(scope: Scope<*>): Boolean
    }

    data class Scope<T: Node>(
        val node: T,
        val variables: Set<String>,
        val enclosed: Set<Scope<*>>
    ) {
        var parent: Scope<*>? = null
            private set

        init {
            enclosed.forEach { it.parent = this }
        }

        fun accept(visitor: ScopeVisitor) {
            if (visitor.visit(this))
                enclosed.forEach { it.accept(visitor) }
        }

        fun getRootScope(): Scope<*> {
            var current: Scope<*> = this
            while (current.parent != null)
                current = current.parent!!
            return current
        }

        @Suppress("UNCHECKED_CAST")
        inline fun <reified R : Node> findAncestor(): Optional<Scope<R>> {
            var current: Scope<*>? = this
            while (current != null && current.node !is R)
                current = current.parent
            return Optional.ofNullable(current as? Scope<R>)
        }

        fun contains(identifier: String): Boolean =
            identifier in variables

        fun getUsableVariables(): Set<String> {
            val usable = mutableSetOf<String>()

            var current: Scope<*>? = this
            while (current != null) {
                usable.addAll(current.variables)
                current = current.parent
            }

            return usable
        }

        @Suppress("UNCHECKED_CAST")
        fun <R : Node> findDeclaringScopeInHierarchy(node: R): Optional<Scope<R>> {
            val queue = mutableListOf<Scope<*>>(this)
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                val contains = current.node.findAll(node::class.java).contains(node)
                val notInChildren = current.enclosed.none { it.node.findAll(node::class.java).contains(node) }
                if (current.node == node || (contains && notInChildren))
                    return Optional.of(current as Scope<R>)
                else
                    queue.addAll(current.enclosed)
            }
            return Optional.empty()
        }

        fun findDeclaringScopeInHierarchy(identifier: String): Optional<Scope<*>> {
            val queue = mutableListOf<Scope<*>>(this)
            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                if (current.contains(identifier))
                    return Optional.of(current)
                else
                    queue.addAll(current.enclosed)
            }
            return Optional.empty()
        }

        fun pretty(): String {
            fun print(scope: Scope<*>, depth: Int): String {
                val tabs = "\t".repeat(depth)
                val self = "$tabs${scope.node::class.simpleName}: {${scope.variables.joinToString()}}"
                return if (scope.enclosed.isEmpty()) self
                else self + System.lineSeparator() + scope.enclosed.joinToString(System.lineSeparator()) {
                    print(it, depth + 1)
                }
            }
            return print(this, 0)
        }
    }

    fun <T : Node> get(node: T): Scope<T> = when (node) {
        is CompilationUnit -> Scope(
            node,
            emptySet(),
            node.types.map { get(it) }.toSet()
        )

        is ClassOrInterfaceDeclaration -> Scope(
            node,
            node.fields.flatMap { it.variables.map { v -> v.nameAsString } }.toSet(),
            node.methods.map { get(it) }.union(node.constructors.map { get(it) })
        )

        is RecordDeclaration -> Scope(
            node,
            node.fields.flatMap { it.variables.map { v -> v.nameAsString } }.toSet(),
            node.methods.map { get(it) }.union(node.constructors.map { get(it) }).union(node.compactConstructors.map { get(it) })
        )

        is ConstructorDeclaration -> Scope(
            node,
            node.parameters.map { it.nameAsString }.toSet(),
            setOf(get(node.body))
        )

        is CompactConstructorDeclaration -> Scope(
            node,
            emptySet(),
            setOf(get(node.body))
        )

        is MethodDeclaration -> Scope(
            node,
            node.parameters.map { it.nameAsString }.toSet(),
            if (node.body.isPresent) setOf(get(node.body.get())) else emptySet(),
        )

        is BlockStmt -> Scope(
            node,
            node.childNodes.filterIsInstance<VariableDeclarationExpr>().flatMap { it.variables.map { v -> v.nameAsString } }.toSet(),
            node.childNodes.map { get(it) }.toSet()
        )

        is ForStmt -> Scope(
            node,
            node.initialization.filterIsInstance<VariableDeclarationExpr>().flatMap { it.variables.map { v -> v.nameAsString } }.toSet(),
            setOf(get(node.body))
        )

        is ForEachStmt -> Scope(
            node,
            setOf(node.variableDeclarator.nameAsString),
            setOf(get(node.body))
        )

        else -> Scope(node, setOf(), setOf())
    }
}