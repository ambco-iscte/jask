package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.correctAndRandomDistractors
import pt.iscte.pesca.extensions.getLocalVariables
import pt.iscte.pesca.questions.subtypes.JavaParserQuestionRandomMethod
import pt.iscte.strudel.parsing.java.SourceLocation

class HowManyVariables : JavaParserQuestionRandomMethod() {

    override fun build(
        source: SourceCode,
        method: MethodDeclaration,
        language: Language
    ): QuestionData {
        val localVariables = method.getLocalVariables()
        val howManyVariables = localVariables.size

        return QuestionData(
            source,
            TextWithCodeStatement(language["HowManyVariables"].format(method.nameAsString), method.toString()),
            correctAndRandomDistractors(
                howManyVariables,
                setOf(method.parameters.size, method.parameters.size + howManyVariables, 0, 1, 2, 3, 4),
                3
            ),
            language = language,
            relevantSourceCode = localVariables.map { SourceLocation(it) }
        )
    }
}