package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.MethodCallExpr
import jdk.jfr.Description
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.findAll
import pt.iscte.jask.extensions.formatted
import pt.iscte.jask.extensions.hasMethodCalls
import pt.iscte.jask.extensions.trueOrFalse
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.parsing.java.extensions.getOrNull

class IsRecursive : StructuralQuestionTemplate<MethodDeclaration>() {

    companion object {
        fun options(functionName: String, isRecursive: Boolean, recursiveCalls: String, language: Language): Map<Option, Boolean> {
            val options = mutableMapOf<Option, Boolean>()

            if (isRecursive) {
                options[SimpleTextOption.yes(
                    language,
                    language["IsRecursive_YesCorrect"].format(functionName, recursiveCalls)
                )] = true

                options[SimpleTextOption.no(
                    language,
                    language["IsRecursive_NoIncorrect"].format()
                )] = false
            } else {
                options[SimpleTextOption.no(
                    language,
                    language["IsRecursive_NoCorrect"].format(functionName)
                )] = true

                options[SimpleTextOption.yes(
                    language,
                    language["IsRecursive_YesIncorrect"].format()
                )] = false
            }

            return options
        }
    }

    @Description("Method must contain at least 1 method call")
    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.body.getOrNull?.hasMethodCalls() == true

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val recursiveCalls = method.findAll<MethodCallExpr>().filter { call ->
            call.nameAsString == method.nameAsString
        }
        val isRecursive = recursiveCalls.isNotEmpty()

        val options = options(method.nameAsString, isRecursive, recursiveCalls.joinToString(), language)

        return Question(
            source,
            TextWithCodeStatement(language["IsRecursive"].format(method.nameAsString), method.toString()),
            options,
            language = language,
            relevantSourceCode = recursiveCalls.map { SourceLocation(it) }
        )
    }
}

fun main() {
    val source = """
        class Test {
            static int factorial(int n) {
                if (n == 0) return 1;
                return n * factorial(n - 1);
            }
        }
    """.trimIndent()

    val template = IsRecursive()
    val qlc = template.generate(source)
    println(qlc)
}