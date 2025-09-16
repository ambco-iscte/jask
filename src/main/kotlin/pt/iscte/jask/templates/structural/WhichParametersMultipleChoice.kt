package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getLocalVariables
import pt.iscte.jask.extensions.sample
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.extensions.toSetBy
import pt.iscte.strudel.parsing.java.SourceLocation
import kotlin.collections.plus

class WhichParametersMultipleChoice : StructuralQuestionTemplate<MethodDeclaration>() {

    // Method has at least one parameter or local variable.
    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.parameters.isNotEmpty() || element.getLocalVariables().isNotEmpty()

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val parameters = method.parameters.map { it.nameAsString }.toSet()
        val paramTypes = method.parameters.map { it.typeAsString }.toSet()

        val localVars = method.getLocalVariables().map { it.nameAsString }.toSet()
        val localVarTypes = method.getLocalVariables().map { it.typeAsString }.toSet()

        val distractors = sampleSequentially(3,
            paramTypes.map { it to language["WhichParametersMultipleChoice_DistractorParamTypes"].format() },
            localVars.map { it to language["WhichParametersMultipleChoice_DistractorLocalVars"].format(method.nameAsString) },
            localVarTypes.map { it to language["WhichParametersMultipleChoice_DistractorLocalVarTypes"].format(method.nameAsString) },
            setOf(method.nameAsString to null)
        ) {
            it.first !in parameters
        }.toSetBy { it.first }

        val options: MutableMap<Option, Boolean> = distractors.associate {
            SimpleTextOption(it.first, it.second) to false
        }.toMutableMap()

        if (parameters.isEmpty())
            options[SimpleTextOption.none(
                language,
                language["WhichParametersMultipleChoice_NoneCorrect"].format(method.nameAsString)
            )] = true
        else parameters.forEach { parameter ->
            options[SimpleTextOption(parameter, null)] = true
        }

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