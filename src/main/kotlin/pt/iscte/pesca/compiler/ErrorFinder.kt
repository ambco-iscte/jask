package pt.iscte.pesca.compiler

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.ConstructorDeclaration
import com.github.javaparser.ast.body.FieldDeclaration
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.RecordDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.type.Type
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import pt.iscte.pesca.compiler.errors.IncompatibleReturnType
import pt.iscte.pesca.compiler.errors.IncompatibleVariableType
import pt.iscte.pesca.compiler.errors.UnknownType
import pt.iscte.pesca.compiler.errors.UnknownMethod
import pt.iscte.pesca.compiler.errors.UnknownVariable
import pt.iscte.pesca.extensions.failure
import pt.iscte.pesca.extensions.success
import pt.iscte.strudel.parsing.java.extensions.getOrNull

interface ICompilerError {
    fun message(): String
}

class ErrorFinder {

    private val unit: CompilationUnit

    constructor(source: String) {
        StaticJavaParser.getParserConfiguration().languageLevel = ParserConfiguration.LanguageLevel.JAVA_20
        StaticJavaParser.getParserConfiguration().setSymbolResolver(
            JavaSymbolSolver(CombinedTypeSolver().apply { add(ReflectionTypeSolver()) })
        )
        this.unit = StaticJavaParser.parse(source)
    }

    fun findUnknownVariables(): List<UnknownVariable> {
        val scope = VariableScoping.get(unit)
        return unit.findAll(NameExpr::class.java).filter {
            failure { it.calculateResolvedType() } // Does this just... work?
        }.map { UnknownVariable(it, scope.findDeclaringScopeInHierarchy(it).get()) }
    }

    fun findUnknownMethods(): List<UnknownMethod> {
        fun getUsableMethods(call: MethodCallExpr): List<MethodDeclaration> {
            return unit.types.flatMap { type ->
                if (type == call.findAncestor(TypeDeclaration::class.java).get()) {
                    if (call.findAncestor(MethodDeclaration::class.java).getOrNull?.isStatic == true) {
                        type.members.filterIsInstance<MethodDeclaration>().filter { it.isStatic }
                    } else
                        type.members.filterIsInstance<MethodDeclaration>()
                }
                else type.members.filterIsInstance<MethodDeclaration>().filter { it.isStatic }
            }
        }

        return unit.findAll(MethodCallExpr::class.java).filter {
            val scope =
                if (it.scope.isPresent) failure { it.scope.get().calculateResolvedType() }
                else true
            failure { it.calculateResolvedType() } && scope
        }.map { UnknownMethod(it, getUsableMethods(it)) }
    }

    fun findUnknownClasses(): List<UnknownType> {
        fun undefined(type: Type): Boolean =
            failure { type.resolve() }

        val unknown = mutableListOf<UnknownType>()

        // Variable Declarations
        unit.findAll(VariableDeclarationExpr::class.java).forEach { declaration ->
            declaration.variables.forEach { variable ->
                if (undefined(variable.type))
                    unknown.add(UnknownType(variable.type, declaration, unit.types))
            }
        }

        // Field Declarations
        unit.findAll(FieldDeclaration::class.java).forEach { declaration ->
            declaration.variables.forEach { variable ->
                if (undefined(variable.type))
                    unknown.add(UnknownType(variable.type, declaration, unit.types))
            }
        }

        // Method Declarations
        unit.findAll(MethodDeclaration::class.java).forEach { method ->
            if (undefined(method.type))
                unknown.add(UnknownType(method.type, method, unit.types))
            method.parameters.forEach { parameter ->
                if (undefined(parameter.type))
                    unknown.add(UnknownType(parameter.type, parameter, unit.types))
            }
        }

        // Constructor Declarations
        unit.findAll(ConstructorDeclaration::class.java).forEach { constructor ->
            constructor.parameters.forEach { parameter ->
                if (undefined(parameter.type))
                    unknown.add(UnknownType(parameter.type, parameter, unit.types))
            }
        }

        // Record Declarations
        unit.findAll(RecordDeclaration::class.java).forEach { record ->
            record.parameters.forEach { parameter ->
                if (undefined(parameter.type))
                    unknown.add(UnknownType(parameter.type, parameter, unit.types))
            }
        }

        return unknown
    }

    fun findIncompatibleVariableTypes(): List<IncompatibleVariableType> =
        unit.findAll(VariableDeclarator::class.java).filter { variable ->
            variable.initializer.isPresent &&
            success { variable.initializer.get().calculateResolvedType() }
            && variable.typeAsString != variable.initializer.get().calculateResolvedType().describe()
        }.map { IncompatibleVariableType(it) }

    fun findIncompatibleReturnTypes(): List<IncompatibleReturnType> =
        unit.findAll(ReturnStmt::class.java).filter {
            val method = it.findAncestor(MethodDeclaration::class.java)
            method.isPresent &&
            it.expression.isPresent &&
            success { it.expression.get().calculateResolvedType() } &&
            it.expression.get().calculateResolvedType().describe() != method.get().typeAsString
        }.map {
            IncompatibleReturnType(it.findAncestor(MethodDeclaration::class.java).get(), it)
        }
}