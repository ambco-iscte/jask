package pt.iscte.jask.errors.compiler.templates

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import pt.iscte.jask.Language
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.WrongTypeForVariableDeclaration
import pt.iscte.jask.extensions.randomBy
import pt.iscte.jask.extensions.toMethodDeclaration
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.jask.templates.*
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.jask.templates.structural.*

class WhichMethodCallReturnType(
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
        val methodUsedToInitialise = initCall.toMethodDeclaration() ?:
        throw ApplicableElementNotFoundException(
            this,
            mapOf(source to NoSuchElementException("Cannot resolve method declaration for method call: $initCall")),
            MethodDeclaration::class
        )

        return Question(
            source,
            TextWithCodeStatement(
                language["WhichReturnType"].format(
                    methodUsedToInitialise.nameAsString,
                ),
                NodeList(VariableDeclarationExpr(error.variable), initCall)
            ),
            WhichReturnType.options(methodUsedToInitialise, language),
            language = language,
            relevantSourceCode = listOf(SourceLocation(error.variable), SourceLocation(initCall))
        )
    }
}