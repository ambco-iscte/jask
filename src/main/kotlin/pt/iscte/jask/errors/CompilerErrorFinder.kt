package pt.iscte.jask.errors

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.type.Type
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import pt.iscte.jask.Language
import pt.iscte.jask.Localisation
import pt.iscte.jask.errors.compiler.MissingReturnInBranch
import pt.iscte.jask.errors.compiler.WrongReturnStmtType
import pt.iscte.jask.errors.compiler.WrongTypeForVariableDeclaration
import pt.iscte.jask.errors.compiler.UnknownType
import pt.iscte.jask.errors.compiler.UnknownMethod
import pt.iscte.jask.errors.compiler.UnknownVariable
import pt.iscte.jask.errors.compiler.WrongMethodCallParameters
import pt.iscte.jask.errors.compiler.templates.WhichMethodCallReturnType
import pt.iscte.jask.errors.compiler.templates.CallMethodWithWrongParameterNumber
import pt.iscte.jask.errors.compiler.templates.CallMethodWithWrongParameterTypes
import pt.iscte.jask.errors.compiler.templates.WhichReturnStmtType
import pt.iscte.jask.errors.compiler.templates.WhichVariableType
import pt.iscte.jask.errors.compiler.templates.WhichWrongReturnStmtTypeMethodReturnType
import pt.iscte.jask.errors.compiler.templates.WhichVariablesUsableAtLine
import pt.iscte.jask.extensions.configureStaticJavaParser
import pt.iscte.jask.extensions.failure
import pt.iscte.jask.extensions.findAllTypes
import pt.iscte.jask.extensions.findClosestMethodDeclaration
import pt.iscte.jask.extensions.isValidFor
import pt.iscte.jask.extensions.nameWithScope
import pt.iscte.jask.extensions.success
import pt.iscte.jask.extensions.toType
import pt.iscte.jask.common.QuestionSequenceWithContext
import pt.iscte.jask.common.SimpleTextStatement
import pt.iscte.jask.errors.compiler.templates.HowManyMethodCallArguments
import pt.iscte.jask.errors.compiler.templates.HowManyReturnStatements
import pt.iscte.jask.errors.compiler.templates.WhichMethodCallArgumentTypes
import pt.iscte.jask.errors.compiler.templates.WhichReturnExecutesOnCondition
import pt.iscte.jask.extensions.ControlFlowGraph
import pt.iscte.jask.extensions.toStringPretty
import pt.iscte.jask.templates.structural.WhatVariables
import pt.iscte.jask.templates.structural.WhichParametersSingleChoice
import pt.iscte.strudel.parsing.java.extensions.getOrNull

interface ICompilerError {
    fun message(): String
    fun toQLC(unit: CompilationUnit, language: Language): QuestionSequenceWithContext?
}

class CompilerErrorFinder<T : Node>(
    private val target: T,
    private val language: Language = Localisation.DefaultLanguage
) {

    init {
        require(target.findCompilationUnit().isPresent) {
            "No compilation unit found for ${target::class.simpleName}: $target"
        }
        configureStaticJavaParser(target.findCompilationUnit().getOrNull)
    }

    private val unit: CompilationUnit =
        (target as? CompilationUnit) ?: target.findCompilationUnit().get()

    // TODO: prettify
    fun findAll(): List<ICompilerError> =
        findUnknownVariables() +
        findUnknownMethods() +
        findUnknownClasses() +
        findVariablesAssignedWithWrongType() +
        findReturnStmtsWithWrongType() +
        findMethodCallsWithWrongArguments() +
        findMissingReturnInBranch()

    fun findAllAndGenerateQLCs(): List<QuestionSequenceWithContext> = findAll().mapNotNull { error ->
        runCatching {
            error.toQLC(unit, language)
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }

    fun findMissingReturnInBranch(): List<MissingReturnInBranch> {
        val t = when (target) {
            is MethodDeclaration ->
                target

            is CompilationUnit if target.types.singleOrNull()?.methods?.size == 1 ->
                target.types.single().methods.single()

            else ->
                return emptyList()
                /*
                throw UnsupportedOperationException(
                    ("Compiler error finder target AST element for MissingReturnInBranch errors must be of" +
                            " type <%s> or a <%s> with only one type containing a single method, but a <%s> was provided").format(
                        MethodDeclaration::class.java.canonicalName,
                        CompilationUnit::class.java.canonicalName,
                        target::class.java.canonicalName)
                )
                 */
        }

        val flow = ControlFlowGraph(t)
        return flow.findBranchesWithMissingReturn().map { branch ->
            MissingReturnInBranch(t, flow, branch)
        }
    }

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

        return target.findAllTypes().filter { undefined(it.second) }.map {
            UnknownType(it.second, it.first, unit.types.map { t -> t.toType() })
        }
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
            success { method.get().type.resolve() } &&
            it.expression.isPresent &&
            success { it.expression.get().calculateResolvedType() } &&
            // it.expression.get().calculateResolvedType().describe() != method.get().type.resolve().describe() &&
            !method.get().type.resolve().isAssignableBy(it.expression.get().calculateResolvedType())
        }.map {
            WrongReturnStmtType(it.findAncestor(MethodDeclaration::class.java).get(), it)
        }

    fun findMethodCallsWithWrongArguments(): List<WrongMethodCallParameters> =
        target.findAll(MethodCallExpr::class.java).filter {
            val dec = it.findClosestMethodDeclaration()
            dec.isPresent && !it.isValidFor(dec.get())
        }.map { WrongMethodCallParameters(it.findClosestMethodDeclaration().get(), it) }
}

fun main() {
    val src = """
        class test {
            static int[] merge(int[] a, int[] b) {
                if (a.length != b.length) {
                    return null;
                }
            
                int n = a.length;
                int[] c = new int[n];
            
                for (int i = 0; i < n; i++) {
                    if (a[i] < 0) {
                        int inverse = -a[i];
                        c[i] = inverse + b[i];
                    } else {
                        c[i] = inverse - b[i];
                    }
                }
            
                return c;
            }
        }
    """.trimIndent()
    val unit = StaticJavaParser.parse(src)
    CompilerErrorFinder(unit).findAllAndGenerateQLCs().forEach {
        println(it)
    }
}