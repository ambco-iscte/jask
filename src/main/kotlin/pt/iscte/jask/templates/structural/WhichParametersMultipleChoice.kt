package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getLocalVariables
import pt.iscte.jask.extensions.sample
import pt.iscte.strudel.parsing.java.SourceLocation
import kotlin.collections.plus

class WhichParametersMultipleChoice : StaticQuestionTemplate<MethodDeclaration>() {

    // Method has at least one parameter or local variable.
    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.parameters.isNotEmpty() || element.getLocalVariables().isNotEmpty()

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val parameters = method.parameters.map { it.nameAsString }
        val paramTypes = method.parameters.map { it.typeAsString }

        val localVars = method.getLocalVariables().map { it.nameAsString }
        val localVarTypes = method.getLocalVariables().map { it.typeAsString }

        val options: Map<Option, Boolean> =
            parameters.associate { SimpleTextOption(it) to true } +
            (localVars + paramTypes + localVarTypes).sample(4).associate { SimpleTextOption(it) to false }

        return Question(
            source,
            TextWithCodeStatement(language["WhichParameters"].format(method.nameAsString), method),
            options,
            language = language,
            choice = QuestionChoiceType.MULTIPLE,
            relevantSourceCode = method.parameters.map { SourceLocation(it) }
        )
    }
}