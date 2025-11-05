package pt.iscte.jask.errors.compiler.templates

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import pt.iscte.jask.Language
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.WrongTypeForVariableDeclaration
import pt.iscte.jask.extensions.findMethodDeclaration
import pt.iscte.jask.extensions.nameWithScope
import pt.iscte.jask.extensions.randomBy
import pt.iscte.jask.templates.*
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.jask.templates.structural.*

class AssignVarWithMethodWrongType(
    private val error: WrongTypeForVariableDeclaration? = null
): StructuralQuestionTemplate<TypeDeclaration<*>>() {

    init {
        if (error != null)
            require(error.initialiserIsMethodCall)
    }

    override fun isApplicable(element: TypeDeclaration<*>): Boolean =
        if (error == null)
            CompilerErrorFinder(element).findVariablesAssignedWithWrongType().any {
                it.initialiserIsMethodCall
            }
        else
            element.isAncestorOf(error.variable)

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, clazz) = sources.getRandom<TypeDeclaration<*>>()

        val error = this.error ?: CompilerErrorFinder(clazz).findVariablesAssignedWithWrongType().randomBy {
            it.initialiserIsMethodCall
        }

        val initCall = error.variableInitialiser.asMethodCallExpr()
        val methodUsedToInitialise = initCall.findMethodDeclaration().orElseThrow {
            ApplicableProcedureCallNotFoundException(
                this@AssignVarWithMethodWrongType,
                mapOf(source to listOf(NoSuchMethodException("Could not find method ${initCall.nameWithScope()} in: $initCall")))
            )
        }

        return Question(
            source,
            TextWithCodeStatement(
                language["AssignVarWithMethodWrongType"].format(
                    error.variableInitialiser.toString(),
                    error.variable.nameAsString,
                    methodUsedToInitialise.nameAsString,
                ),
                NodeList(VariableDeclarationExpr(error.variable), methodUsedToInitialise)
            ),
            WhichReturnType.options(methodUsedToInitialise, language),
            language = language,
            relevantSourceCode = listOf(SourceLocation(error.variable), SourceLocation(methodUsedToInitialise.type))
        )
    }
}