package pt.iscte.jask.templates.structural
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.AccessSpecifier
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.NodeList
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.stmt.DoStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.IfStmt
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.stmt.WhileStmt
import com.github.javaparser.ast.type.VoidType
import jdk.jfr.Description
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.findAll
import pt.iscte.jask.extensions.getLocalVariables
import pt.iscte.jask.extensions.getLoopControlStructures
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.extensions.toSetBy
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.parsing.java.extensions.getOrNull
import kotlin.collections.emptyList

class WhichFunctionDependencies : StructuralQuestionTemplate<MethodDeclaration>() {

    @Description("Method must contain at least 1 method call to another method")
    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.findAll(MethodCallExpr::class.java).any {
            it.nameAsString != element.nameAsString
        }

    private fun MethodCallExpr.toMethodDeclaration(): MethodDeclaration {
        fun AccessSpecifier.toModifier(): Modifier? = when (this) {
            AccessSpecifier.PUBLIC -> Modifier.publicModifier()
            AccessSpecifier.PRIVATE -> Modifier.privateModifier()
            AccessSpecifier.PROTECTED -> Modifier.privateModifier()
            AccessSpecifier.NONE -> null
        }

        return runCatching {
            val resolved = this.resolve()

            val returnType = StaticJavaParser.parseType(resolved.returnType.describe())

            val modifiers = NodeList.nodeList<Modifier>()
            resolved.accessSpecifier().toModifier()?.let { modifiers.add(it) }
            if (resolved.isStatic) modifiers.add(Modifier.staticModifier())
            if (resolved.isAbstract) modifiers.add(Modifier.abstractModifier())

            return MethodDeclaration(modifiers, returnType, nameAsString)
        }.getOrDefault(
            defaultValue = MethodDeclaration(NodeList.nodeList(), VoidType(), nameAsString) // ugly
        )
    }

    private fun getDependencies(method: MethodDeclaration, unit: CompilationUnit? = null): Set<MethodDeclaration> {
        val unit = unit ?: method.findCompilationUnit().getOrNull ?: return emptySet()
        return method.findAll(MethodCallExpr::class.java).mapNotNull { call ->
            if (call.nameAsString == method.nameAsString) null
            else unit.findFirst(MethodDeclaration::class.java) {
                it.nameAsString == call.nameAsString
            }.getOrNull ?: call.toMethodDeclaration()
        }.toSet()
    }

    private fun getDependenciesDeep(method: MethodDeclaration, unit: CompilationUnit? = null): Set<MethodDeclaration> {
        val unit = unit ?: method.findCompilationUnit().getOrNull ?: return emptySet()

        val dependencies = getDependencies(method, unit).toMutableSet()

        val visited = mutableMapOf<MethodDeclaration, Boolean>()
        val queue = dependencies.toMutableList()

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            visited[current] = true
            getDependencies(current, unit).forEach { dependency ->
                dependencies.add(dependency)
                if (dependency !in visited)
                    queue.add(dependency)
            }
        }

        return dependencies
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val dependencyNames = getDependencies(method).map { it.nameAsString }.toSet()
        val dependencyScopeNames = method.findAll(MethodCallExpr::class.java).mapNotNull {
            it.scope.getOrNull?.toString()
        }.toSet()
        val dependenciesDeep = getDependenciesDeep(method)
        val dependencyNamesDeep = dependenciesDeep.map { it.nameAsString }.toSet()

        val deepDependenciesClasses = dependenciesDeep.mapNotNull {
            it.findAncestor<TypeDeclaration<*>>().getOrNull?.nameAsString
        }
        val thisClass = method.findAncestor<TypeDeclaration<*>>().getOrNull?.nameAsString?.let { listOf(it) } ?: emptyList()

        val controlStructures = method.getLoopControlStructures().map { (control, _) ->
            when (control) {
                is IfStmt -> "if"
                is WhileStmt, is DoStmt -> "while"
                is ForStmt, is ForEachStmt -> "for"
                else -> toString()
            }
        }.toSet()

        val classes = (
            method.findCompilationUnit().getOrNull?.findAll(TypeDeclaration::class.java) ?: emptyList()
        ).map { it.nameAsString!! }.toSet()

        val otherFunctions = (
            method.findCompilationUnit().getOrNull?.findAll(MethodDeclaration::class.java) ?: emptyList()
        ).minus(method).map { it.nameAsString!! }.toSet()

        val returns =
            if (method.findFirst(ReturnStmt::class.java).isPresent) setOf("return")
            else emptySet()

        val types = listOf(method.type.toString()).plus(method.getLocalVariables().map { it.typeAsString }).plus(method.parameters.map { it.typeAsString }).toSet()

        val modifiers = method.modifiers.map { it.keyword.asString() }.toSet()

        val distractors = sampleSequentially(3, listOf(
            dependencyNames.plus(method.nameAsString) to null,
            dependencyNames.plus(dependencyScopeNames) to null,
            dependencyScopeNames to null
        ),
        listOf(
            dependencyNames.plus(thisClass) to null,
            dependencyNamesDeep to null,
            dependencyNamesDeep.plus(method.nameAsString) to null,
            dependencyNamesDeep.plus(deepDependenciesClasses) to null
        ),
        listOf(
            dependencyNames.plus(thisClass).plus(method.nameAsString) to null,
            dependencyNamesDeep.plus(deepDependenciesClasses).plus(method.nameAsString) to null,
            dependencyNames.plus(classes) to null,
            dependencyNames.plus(otherFunctions) to null,
            dependencyNames.plus(otherFunctions).plus(classes) to null
        ),
        listOf(
            otherFunctions.plus(method.nameAsString) to null,
            classes.plus(method.nameAsString) to null,
            listOf(method.nameAsString) to null
        ), listOf(
            controlStructures to language["WhichFunctionDependencies_DistractorControlStructures"].format(controlStructures.joinToString()),
            dependencyNames.plus(controlStructures) to language["WhichFunctionDependencies_DistractorNamesAndControlStructures"].format(controlStructures.joinToString()),
            returns to language["WhichFunctionDependencies_DistractorReturnStmts"].format("return"),
            controlStructures.plus(returns) to language["WhichFunctionDependencies_DistractorControlsAndReturns"].format(controlStructures.joinToString(), "return"),
            dependencyNames.plus(returns) to null,
            dependencyNames.plus(controlStructures).plus(returns) to null,
            types to language["WhichFunctionDependencies_DistractorTypes"].format(),
            modifiers to language["WhichFunctionDependencies_DistractorModifiers"].format("public", "static"),
            types.plus(modifiers) to null,
            dependencyNames.plus(types) to null,
            dependencyNames.plus(modifiers) to null
        )) {
            it.first.toSet().isNotEmpty() && it.first.toSet() != dependencyNames.toSet()
        }.toSetBy { it.first.toSet() }

        val options: MutableMap<Option, Boolean> = distractors.associate {
            SimpleTextOption(it.first, it.second) to false
        }.toMutableMap()

        options[SimpleTextOption(
            dependencyNames,
            language["WhichFunctionDependencies_Correct"].format()
        )] = true

        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return Question(
            source,
            TextWithCodeStatement(language["WhichFunctionDependencies"].format(method.nameAsString), method),
            options,
            language = language,
            relevantSourceCode = method.findAll<MethodCallExpr>().map { SourceLocation(it) }
        )
    }
}