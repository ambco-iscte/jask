package pt.iscte.jask.extensions

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.Position
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.AccessSpecifier
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Modifier
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.*
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.nodeTypes.NodeWithBody
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName
import com.github.javaparser.ast.nodeTypes.NodeWithTypeArguments
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.*
import com.github.javaparser.ast.visitor.GenericVisitor
import com.github.javaparser.ast.visitor.VoidVisitor
import com.github.javaparser.resolution.Context
import com.github.javaparser.resolution.UnsolvedSymbolException
import com.github.javaparser.resolution.declarations.ResolvedReferenceTypeDeclaration
import com.github.javaparser.resolution.declarations.ResolvedTypeDeclaration
import com.github.javaparser.resolution.declarations.ResolvedTypeParameterDeclaration
import com.github.javaparser.resolution.declarations.ResolvedTypeParametrizable
import com.github.javaparser.resolution.model.typesystem.ReferenceTypeImpl
import com.github.javaparser.resolution.types.ResolvedType
import com.github.javaparser.resolution.types.ResolvedTypeVariable
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.javaparsermodel.declarations.JavaParserMethodDeclaration
import com.github.javaparser.symbolsolver.reflectionmodel.ReflectionMethodDeclaration
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import pt.iscte.strudel.parsing.java.extensions.getOrNull
import java.util.*
import kotlin.collections.map
import kotlin.jvm.optionals.getOrDefault
import kotlin.jvm.optionals.getOrNull
import kotlin.math.abs

fun configureStaticJavaParser(
    target: CompilationUnit? = null,
    languageLevel: ParserConfiguration.LanguageLevel = ParserConfiguration.LanguageLevel.JAVA_20
) {
    StaticJavaParser.getParserConfiguration().languageLevel = languageLevel

    val resolver = JavaSymbolSolver(CombinedTypeSolver().apply { add(ReflectionTypeSolver()) })
    StaticJavaParser.getParserConfiguration().setSymbolResolver(resolver)

    if (target != null)
        resolver.inject(target)
}

fun AccessSpecifier.toModifier(): Modifier? = when (this) {
    AccessSpecifier.PUBLIC -> Modifier.publicModifier()
    AccessSpecifier.PRIVATE -> Modifier.privateModifier()
    AccessSpecifier.PROTECTED -> Modifier.privateModifier()
    AccessSpecifier.NONE -> null
}

val ReflectionMethodDeclaration.nameWithSimpleScope: String
    get() = "${declaringType().name}.${name}"

fun MethodCallExpr.toMethodDeclaration(): MethodDeclaration? =
    runCatching {
        return when (val resolved = this.resolve()) {
            is JavaParserMethodDeclaration -> resolved.wrappedNode
            else -> {
                val returnType = StaticJavaParser.parseType(resolved.returnType.describe())

                val modifiers = NodeList.nodeList<Modifier>()
                resolved.accessSpecifier().toModifier()?.let { modifiers.add(it) }
                if (resolved.isStatic) modifiers.add(Modifier.staticModifier())
                if (resolved.isAbstract) modifiers.add(Modifier.abstractModifier())

                val name = (resolved as? ReflectionMethodDeclaration)?.nameWithSimpleScope ?: nameAsString

                MethodDeclaration(modifiers, returnType, name)
            }
        }
    }.getOrNull()

fun ResolvedTypeDeclaration.toResolvedType(): ResolvedType = object : ResolvedType {
    override fun describe(): String? =
        this@toResolvedType.qualifiedName

    override fun isAssignableBy(other: ResolvedType?): Boolean {
        val typeParameters = if (this@toResolvedType is ResolvedTypeParametrizable)
            this@toResolvedType.typeParameters.map { it.toResolvedType() }
        else
            null

        return when (this@toResolvedType) {
            is ResolvedReferenceTypeDeclaration ->
                ReferenceTypeImpl(this@toResolvedType, typeParameters).isAssignableBy(other)

            is ResolvedTypeParameterDeclaration ->
                ResolvedTypeVariable(this@toResolvedType.asTypeParameter()).isAssignableBy(other)

            else -> error("Cannot check assignability of ${describe()}")
        }
    }
}

fun TypeDeclaration<*>.toType(): Type = object : Type(this.tokenRange.get(), this.annotations) {
    override fun asString(): String? =
        this@toType.fullyQualifiedName.getOrDefault(this@toType.nameAsString)

    override fun resolve(): ResolvedType =
        this@toType.resolve().toResolvedType()

    override fun <R : Any?, A : Any?> accept(v: GenericVisitor<R?, A?>?, arg: A?): R? = null

    override fun <A : Any?> accept(v: VoidVisitor<A?>?, arg: A?) { }

    override fun convertToUsage(context: Context?): ResolvedType {
        val name = this@toType.fullyQualifiedName.getOrDefault(this@toType.nameAsString)

        val typeParameters = if (this@toType is NodeWithTypeArguments<*> && this@toType.typeArguments.isPresent) {
            this@toType.typeArguments.get().map { it.convertToUsage(context) }
        } else null

        val ref = context?.solveType(name, typeParameters)

        if (ref == null || !ref.isSolved)
            throw UnsolvedSymbolException(name)

        return when (val typeDeclaration = ref.correspondingDeclaration) {
            is ResolvedReferenceTypeDeclaration ->
                ReferenceTypeImpl(typeDeclaration, typeParameters)

            is ResolvedTypeParameterDeclaration ->
                ResolvedTypeVariable(typeDeclaration.asTypeParameter())

            else -> error("Cannot convert declaration of ${asString()} to a Type instance")
        }
    }
}

fun Node.findAllTypes(): List<Pair<Node, Type>> {
    val types = mutableListOf<Pair<Node, Type>>()

    // Type Declarations
    this.findAll(TypeDeclaration::class.java).forEach { declaration ->
        types.add(declaration to declaration.toType())
    }

    // Variable Declarations
    this.findAll(VariableDeclarationExpr::class.java).forEach { declaration ->
        declaration.variables.forEach { variable ->
            types.add(variable to variable.type)
        }
    }

    // Field Declarations
    this.findAll(FieldDeclaration::class.java).forEach { declaration ->
        declaration.variables.forEach { variable ->
            types.add(variable to variable.type)
        }
    }

    // Method Declarations
    this.findAll(MethodDeclaration::class.java).forEach { method ->
        types.add(method to method.type)
        method.parameters.forEach { parameter ->
            types.add(parameter to parameter.type)
        }
    }

    // Constructor Declarations
    this.findAll(ConstructorDeclaration::class.java).forEach { constructor ->
        constructor.parameters.forEach { parameter ->
            types.add(parameter to parameter.type)
        }
    }

    // Record Declarations
    this.findAll(RecordDeclaration::class.java).forEach { record ->
        record.parameters.forEach { parameter ->
            types.add(parameter to parameter.type)
        }
    }

    return types
}

inline fun <reified T : Node> find(source: String, condition: (T) -> Boolean = { true }): List<T> =
    StaticJavaParser.parse(source).findAll(T::class.java).filter { condition(it) }

inline fun <reified T : Node> Node.findAll(noinline condition: (T) -> Boolean = { true }): List<T> =
    findAll(T::class.java, condition)

val MethodDeclaration.prettySignature: String
    get() = "$typeAsString $nameAsString(${parameters.joinToString()})"

fun Node.hasMethodCalls(): Boolean =
    findAll(MethodCallExpr::class.java).isNotEmpty()

fun Node.getLoopControlStructures(): List<Pair<NodeWithBody<*>, Expression?>> =
    findAll(ForStmt::class.java).map { it to it.compare.getOrNull } +
    findAll(DoStmt::class.java).map { it to it.condition } +
    findAll(WhileStmt::class.java).map { it to it.condition } +
    findAll(ForEachStmt::class.java).map { it to null }

fun Node.hasLoopControlStructures(): Boolean =
    getLoopControlStructures().isNotEmpty()

fun Node.getBranches(): List<IfStmt> =
    findAll(IfStmt::class.java)

val JAVA_PRIMITIVE_TYPES: Set<String> =
    PrimitiveType.Primitive.values().map { it.name.lowercase(Locale.getDefault()) }.toSet()

fun MethodDeclaration.returnsPrimitiveOrArrayOrString(): Boolean =
    type.isPrimitiveType || type.isArrayType || type.toString() == String::class.simpleName

fun MethodDeclaration.getUsedTypes(): Set<Type> =
    setOf(type) +
    parameters.map { it.type } +
    findAll(VariableDeclarationExpr::class.java).flatMap { it.variables.map { v -> v.type } }

fun MethodDeclaration.getReturnVariables(): Map<ReturnStmt, List<NodeWithSimpleName<*>>> =
    body.getOrNull()?.findAll(ReturnStmt::class.java)?.associateWith { ret ->
        ret.findAll(Node::class.java).filterIsInstance<NodeWithSimpleName<*>>()
    } ?: emptyMap()

fun MethodDeclaration.getLocalVariables(): List<VariableDeclarator> =
    body.getOrNull()?.findAll(VariableDeclarationExpr::class.java)?.flatMap { it.variables } ?: emptyList()

fun MethodDeclaration.getUsableVariables(): List<VariableDeclarator> =
    getLocalVariables() + (findAncestor(TypeDeclaration::class.java).getOrNull?.findAll(FieldDeclaration::class.java)
        ?.flatMap {
            it.variables
        } ?: listOf())

val MethodDeclaration.isMain: Boolean
    get() = isStatic && type is VoidType && nameAsString == "main" && (parameters.isEmpty() || (parameters.size == 1 && ((parameters[0].type as? ArrayType)?.componentType as? ClassOrInterfaceType)?.nameAsString == String::class.java.canonicalName))

fun MethodCallExpr.nameWithScope(): String {
    runCatching { (resolve() as? ReflectionMethodDeclaration)?.qualifiedName }.getOrNull()?.let { return it }

    if (scope.isPresent) {
        if (findCompilationUnit().isPresent) {
            val unit = findCompilationUnit().get()
            if (unit.types.singleOrNull()?.nameAsString == scope.get().toString())
                return nameAsString
            return "${scope.get()}.$nameAsString"
        }
    }
    return nameAsString
}

fun MethodDeclaration.nameWithScope(): String {
    runCatching { (resolve() as? ReflectionMethodDeclaration)?.qualifiedName }.getOrNull()?.let { return it }

    val type = findAncestor(TypeDeclaration::class.java)
    if (type.isPresent) {
        if (findCompilationUnit().isPresent) {
            val unit = findCompilationUnit().get()
            if (unit.types.singleOrNull() == type.get())
                return nameAsString
            return "${type.get().nameAsString}.$nameAsString"
        }
        "${type.get().nameAsString}.${nameAsString}"
    }
    return nameAsString
}

fun MethodCallExpr.findMethodDeclaration(): Optional<MethodDeclaration> {
    runCatching { (resolve() as? JavaParserMethodDeclaration)?.wrappedNode }.getOrNull()?.let {
        return Optional.of(it)
    }

    val unit = findCompilationUnit()
    if (unit.isEmpty)
        return Optional.empty<MethodDeclaration>()

    return Optional.ofNullable(unit.get().findAll(MethodDeclaration::class.java).firstOrNull {
        (if (this.scope.isPresent)
            it.nameWithScope() == this.nameWithScope()
        else
            it.nameAsString == this.nameAsString
        ) && it.parameters.size == this.arguments.size
    })
}

fun MethodCallExpr.findClosestMethodDeclaration(): Optional<MethodDeclaration> {
    runCatching { (resolve() as? JavaParserMethodDeclaration)?.wrappedNode }.getOrNull()?.let {
        return Optional.of(it)
    }

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

fun MethodCallExpr.isCallFor(method: MethodDeclaration): Boolean =
    runCatching {
        val resolved = this.resolve()
        resolved == method.resolve() || (resolved as? JavaParserMethodDeclaration)?.wrappedNode == method
    }.getOrDefault(false)

fun Position.relativeTo(other: Position): Position =
    Position(line - other.line + 1, column - other.column + 1)

fun MethodDeclaration.hasDuplicatedIfElse(): Boolean =
    this.findAll(IfStmt::class.java).any { ifStmt ->
        ifStmt.hasDuplicateCode()
    }

fun Node.lineRelativeTo(other: Node): Int =
    range.get().begin.relativeTo(other.range.get().begin).line

val Node.line: Int?
    get() = this.range.getOrNull?.begin?.line