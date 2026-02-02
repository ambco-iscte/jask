package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getLocalVariables
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.extensions.toSetBy
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.QuestionOption
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.strudel.parsing.java.SourceLocation

class HowManyVariables : StructuralQuestionTemplate<MethodDeclaration>() {

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val localVariables = method.getLocalVariables()
        val howManyVariables = localVariables.size

        val distractors = sampleSequentially(3, listOf(
            method.parameters.size to (
                if (method.parameters.isEmpty()) null
                else language["HowManyVariables_DistractorParameters"].format(
                    method.nameAsString, method.parameters.joinToString { it.nameAsString }
                )
            ),
            method.parameters.size + howManyVariables + 1 to null,
            method.parameters.size + howManyVariables - 1 to null,
            method.parameters.size + 1 to null,
            method.parameters.size - 1 to null,
            howManyVariables + 1 to null,
            howManyVariables - 1 to null,
            0 to null
        )) {
            it.first != howManyVariables && it.first > 0
        }.toSetBy { it.first }

        val options: MutableMap<QuestionOption, Boolean> = distractors.associate {
            SimpleTextOption(it.first, it.second) to false
        }.toMutableMap()

        options[SimpleTextOption(
            howManyVariables,
            (
                    if (howManyVariables == 0) language["HowManyVariables_ZeroCorrect"]
                    else language["HowManyVariables_Correct"]
                    ).format(method.nameAsString, localVariables.joinToString { it.nameAsString })
        )] = true

        if (method.parameters.size + howManyVariables > howManyVariables) {
            options.keys.firstOrNull { it.toString() == (method.parameters.size + howManyVariables).toString() }?.let {
                options.remove(it)
            }
            options[SimpleTextOption(
                method.parameters.size + howManyVariables,
                language["HowManyVariables_DistractorParametersPlusVars"].format(method.nameAsString)
            )] = false
        }

        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return Question(
            source,
            TextWithCodeStatement(
                language["HowManyVariables"].orAnonymous(method).format(method.nameAsString),
                method.toString()
            ),
            options,
            language = language,
            relevantSourceCode = localVariables.map { SourceLocation(it) }
        )
    }
}

fun main() {
    val source = """
        class Test {
            static void main(int n) {
                int c = 0;
                c = c + 1;
                c = c + 1;
                c = c + 1;
            }
        }
    """.trimIndent()

    val template = HowManyVariables()
    val qlc = template.generate(source)
    println(qlc)
}