package pt.iscte.jask.errors

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NameExpr
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.type.Type
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
import pt.iscte.jask.templates.structural.WhatVariables
import pt.iscte.jask.templates.structural.WhichParametersSingleChoice
import pt.iscte.strudel.parsing.java.extensions.getOrNull

interface ICompilerError {
    fun message(): String
}

class CompilerErrorFinder<T : Node>(
    private val target: T,
    private val language: Language = Localisation.DefaultLanguage
) {

    init {
        configureStaticJavaParser()
    }

    private val unit: CompilationUnit =
        target.findCompilationUnit().get()

    // TODO: prettify
    fun findAll(): List<ICompilerError> =
        findUnknownVariables() +
        findUnknownMethods() +
        findUnknownClasses() +
        findVariablesAssignedWithWrongType() +
        findReturnStmtsWithWrongType() +
        findMethodCallsWithWrongArguments() +
        findMissingReturnInBranch()

    // TODO: prettify
    fun findAllAndGenerateQLCs(): List<QuestionSequenceWithContext> = findAll().mapNotNull { error -> runCatching {
        when (error) {
            is UnknownVariable -> QuestionSequenceWithContext(
                SimpleTextStatement(language["ReferencesUndefinedVariable"].format(
                    error.expr.nameAsString,
                    error.expr.range.get().begin.line
                )),
                listOf(
                    WhatVariables().generate(unit, language),
                    WhichParametersSingleChoice().generate(unit, language),
                    WhichVariablesUsableAtLine(error).generate(unit, language)
                )
            )

            is UnknownMethod -> null // TODO

            is UnknownType -> null // TODO

            is WrongTypeForVariableDeclaration ->
                if (error.initialiserIsMethodCall)
                    QuestionSequenceWithContext(
                        SimpleTextStatement(language["AssignVarWithMethodWrongType"].format(
                            error.variableInitialiser.toString(),
                            error.variable.nameAsString,
                            error.variableInitialiser.asMethodCallExpr().nameAsString
                        )),
                        listOf(
                            WhichVariableType(error).generate(unit, language),
                            WhichMethodCallReturnType(error).generate(unit, language)
                        )
                    )
                else null

            is WrongReturnStmtType -> QuestionSequenceWithContext(
                SimpleTextStatement(language["MethodWithWrongReturnStmt"].format(
                    error.returnStmt.toString(),
                    error.method.nameWithScope()
                )),
                listOf(
                    WhichWrongReturnStmtTypeMethodReturnType(error).generate(unit, language),
                    WhichReturnStmtType(error).generate(unit, language)
                )
            )

            is WrongMethodCallParameters ->
                if (error.parameterNumberMismatch) QuestionSequenceWithContext(
                    SimpleTextStatement(language["CallMethodWithWrongParameterNumber"].format(
                        error.call.toString(),
                        error.call.range.get().begin.line
                    )),
                    CallMethodWithWrongParameterNumber(error).generate(unit, language)
                )
                else if (error.parameterTypeMismatch) QuestionSequenceWithContext(
                    SimpleTextStatement(language["CallMethodWithWrongParameterTypes"].format(
                        error.call.toString(),
                        error.call.range.get().begin.line
                    )),
                    CallMethodWithWrongParameterTypes(error).generate(unit, language)
                )
                else null

            is MissingReturnInBranch ->
                TODO("Not yet implemented: MissingReturnInBranch QLC")

            else -> null
        }
    }.getOrNull() }

    fun findMissingReturnInBranch(): List<MissingReturnInBranch> {
        return emptyList() // TODO (cfg?)
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
            it.expression.isPresent &&
            success { it.expression.get().calculateResolvedType() } &&
            it.expression.get().calculateResolvedType().describe() != method.get().typeAsString
        }.map {
            WrongReturnStmtType(it.findAncestor(MethodDeclaration::class.java).get(), it)
        }

    fun findMethodCallsWithWrongArguments(): List<WrongMethodCallParameters> =
        target.findAll(MethodCallExpr::class.java).filter {
            val dec = it.findClosestMethodDeclaration()
            dec.isPresent && !it.isValidFor(dec.get())
        }.map { WrongMethodCallParameters(it.findClosestMethodDeclaration().get(), it) }
}