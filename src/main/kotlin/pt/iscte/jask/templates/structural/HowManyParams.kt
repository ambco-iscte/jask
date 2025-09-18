package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getLocalVariables
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.extensions.toSetBy
import pt.iscte.strudel.parsing.java.SourceLocation

class HowManyParams : StructuralQuestionTemplate<MethodDeclaration>() {

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val parameters = method.parameters.size
        val local = method.getLocalVariables().size

        val distractors = sampleSequentially(3, listOf(
            parameters + 2 to null,
            parameters + 1 to null,
            parameters - 1 to null,
            local to (if (local > 0) language["HowManyParams_DistractorLocalVars"].format() else null),
            local + parameters to null,
            local + 2 to null,
            local + 1 to null,
            local - 1 to null
        )) {
            it.first != parameters && it.first >= 0
        }.toSetBy { it.first }

        val options: MutableMap<Option, Boolean> = distractors.associate {
            SimpleTextOption(it.first, it.second) to false
        }.toMutableMap()

        options[SimpleTextOption(
            parameters,
            if (parameters == 0)
                language["HowManyParams_ZeroCorrect"].format(method.nameAsString)
            else
                language["HowManyParams_Correct"].format(method.nameAsString, method.parameters.joinToString { it.nameAsString })
        )] = true

        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return Question(
            source,
            TextWithCodeStatement(language["HowManyParams"].format(method.nameAsString), method),
            options,
            language = language,
            relevantSourceCode = method.parameters.map { SourceLocation(it) }
        )
    }
}

fun main() {
    val source = """
        class Test {
            static int next(int n) {
                return n + 1;
            }
        }
    """.trimIndent()

    val template = HowManyParams()
    val qlc = template.generate(source)
    println(qlc)
}