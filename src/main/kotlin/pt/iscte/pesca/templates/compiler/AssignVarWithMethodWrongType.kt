package pt.iscte.pesca.templates

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import pt.iscte.pesca.Language
import pt.iscte.pesca.errors.CompilerErrorFinder
import pt.iscte.pesca.extensions.findMethodDeclaration
import pt.iscte.pesca.extensions.nameWithScope
import pt.iscte.pesca.extensions.randomBy
import pt.iscte.strudel.parsing.java.SourceLocation

class AssignVarWithMethodWrongType: StaticQuestionTemplate<TypeDeclaration<*>>() {

    override fun isApplicable(element: TypeDeclaration<*>): Boolean =
        CompilerErrorFinder(element).findVariablesAssignedWithWrongType().any { it.initialiserIsMethodCall }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, clazz) = sources.getRandom<TypeDeclaration<*>>()

        val error = CompilerErrorFinder(clazz).findVariablesAssignedWithWrongType().randomBy { it.initialiserIsMethodCall }

        val initCall = error.variableInitialiser.asMethodCallExpr()
        val methodUsedToInitialise = initCall.findMethodDeclaration().orElseThrow {
            QuestionGenerationException(
                this,
                source,
                "Could not find method ${initCall.nameWithScope()} in: $initCall"
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
            WhichReturnType.distractors(methodUsedToInitialise),
            language = language,
            relevantSourceCode = listOf(SourceLocation(error.variable), SourceLocation(methodUsedToInitialise.type))
        )
    }
}