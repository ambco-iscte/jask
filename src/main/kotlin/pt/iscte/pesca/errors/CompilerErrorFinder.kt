package pt.iscte.pesca.errors

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
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
import pt.iscte.pesca.errors.compiler.WrongReturnStmtType
import pt.iscte.pesca.errors.compiler.WrongTypeForVariableDeclaration
import pt.iscte.pesca.errors.compiler.UnknownType
import pt.iscte.pesca.errors.compiler.UnknownMethod
import pt.iscte.pesca.errors.compiler.UnknownVariable
import pt.iscte.pesca.errors.compiler.WrongMethodCallParameters
import pt.iscte.pesca.extensions.configureStaticJavaParser
import pt.iscte.pesca.extensions.failure
import pt.iscte.pesca.extensions.findMethodDeclaration
import pt.iscte.pesca.extensions.isValidFor
import pt.iscte.pesca.extensions.success
import pt.iscte.strudel.parsing.java.extensions.getOrNull

interface ICompilerError {
    fun message(): String
}

class CompilerErrorFinder<T : Node>(private val target: T) {

    init {
        configureStaticJavaParser()
    }

    private val unit: CompilationUnit =
        target.findCompilationUnit().get()

    fun findUnknownVariables(): List<UnknownVariable> {
        val scope = VariableScoping.get(target)
        return target.findAll(NameExpr::class.java).filter {
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

        return target.findAll(MethodCallExpr::class.java).filter {
            val scope =
                if (it.scope.isPresent) failure { it.scope.get().calculateResolvedType() }
                else true
            failure { it.calculateResolvedType() } && scope
        }.map { UnknownMethod(it, getUsableMethods(it)) }
    }

    fun findUnknownClasses(): List<UnknownType> {
        fun undefined(type: Type): Boolean =
            !type.isPrimitiveType && failure { type.resolve() }

        val unknown = mutableListOf<UnknownType>()

        // Variable Declarations
        target.findAll(VariableDeclarationExpr::class.java).forEach { declaration ->
            declaration.variables.forEach { variable ->
                if (undefined(variable.type))
                    unknown.add(UnknownType(variable.type, declaration, unit.types))
            }
        }

        // Field Declarations
        target.findAll(FieldDeclaration::class.java).forEach { declaration ->
            declaration.variables.forEach { variable ->
                if (undefined(variable.type))
                    unknown.add(UnknownType(variable.type, declaration, unit.types))
            }
        }

        // Method Declarations
        target.findAll(MethodDeclaration::class.java).forEach { method ->
            if (undefined(method.type))
                unknown.add(UnknownType(method.type, method, unit.types))
            method.parameters.forEach { parameter ->
                if (undefined(parameter.type))
                    unknown.add(UnknownType(parameter.type, parameter, unit.types))
            }
        }

        // Constructor Declarations
        target.findAll(ConstructorDeclaration::class.java).forEach { constructor ->
            constructor.parameters.forEach { parameter ->
                if (undefined(parameter.type))
                    unknown.add(UnknownType(parameter.type, parameter, unit.types))
            }
        }

        // Record Declarations
        target.findAll(RecordDeclaration::class.java).forEach { record ->
            record.parameters.forEach { parameter ->
                if (undefined(parameter.type))
                    unknown.add(UnknownType(parameter.type, parameter, unit.types))
            }
        }

        return unknown
    }

    fun findVariablesAssignedWithWrongType(): List<WrongTypeForVariableDeclaration> =
        target.findAll(VariableDeclarator::class.java).filter { variable ->
            variable.initializer.isPresent &&
            success { variable.initializer.get().calculateResolvedType() }
            && variable.typeAsString != variable.initializer.get().calculateResolvedType().describe()
        }.map { WrongTypeForVariableDeclaration(it) }

    fun findReturnStmtsWithWrongType(): List<WrongReturnStmtType> =
        target.findAll(ReturnStmt::class.java).filter {
            val method = it.findAncestor(MethodDeclaration::class.java)
            method.isPresent &&
            it.expression.isPresent &&
            success { it.expression.get().calculateResolvedType() } &&
            it.expression.get().calculateResolvedType().describe() != method.get().typeAsString
        }.map {
            WrongReturnStmtType(it.findAncestor(MethodDeclaration::class.java).get(), it)
        }

    fun findMethodCallsWithWrongArguments(): List<WrongMethodCallParameters> =
        target.findAll(MethodCallExpr::class.java).filter {
            val dec = it.findMethodDeclaration()
            dec.isPresent && !it.isValidFor(dec.get())
        }.map { WrongMethodCallParameters(it.findMethodDeclaration().get(), it) }
}