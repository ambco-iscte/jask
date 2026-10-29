package pt.iscte.jask.errors.compiler

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.ReturnStmt
import com.github.javaparser.ast.type.Type
import com.github.javaparser.resolution.types.ResolvedReferenceType
import com.github.javaparser.resolution.types.ResolvedType
import pt.iscte.jask.Language
import pt.iscte.jask.common.QuestionSequenceWithContext
import pt.iscte.jask.common.SimpleTextStatement
import pt.iscte.jask.errors.ICompilerError
import pt.iscte.jask.errors.compiler.templates.WhichReturnStmtType
import pt.iscte.jask.errors.compiler.templates.WhichWrongReturnStmtTypeMethodReturnType
import pt.iscte.jask.extensions.nameWithScope
import pt.iscte.jask.extensions.success

data class WrongReturnStmtType(val method: MethodDeclaration, val returnStmt: ReturnStmt): ICompilerError {

    init {
        require(returnStmt.expression.isPresent) { "Return statement must return an expression!" }
        require(success { method.type.resolve() }) {
            "Cannot resolve method return type: ${method.type.asString()}"
        }
        require(success { returnStmt.expression.get().calculateResolvedType() }) {
            "Cannot resolve type of return expression: ${returnStmt.expression.get()}"
        }
    }

    val expected: ResolvedType =
        method.type.resolve()

    val actual: ResolvedType =
        returnStmt.expression.get().calculateResolvedType()

    override fun toQLC(unit: CompilationUnit, language: Language): QuestionSequenceWithContext =
        QuestionSequenceWithContext(
            SimpleTextStatement(language["MethodWithWrongReturnStmt"].format(
                returnStmt.toString(),
                method.nameWithScope()
            )),
            listOf(
                WhichWrongReturnStmtTypeMethodReturnType(this).generate(unit, language),
                WhichReturnStmtType(this).generate(unit, language)
            )
        )

    override fun message(): String =
        "Incompatible return type: expected <${expected.describe()}> but got <${actual.describe()}> in <$returnStmt>"
}