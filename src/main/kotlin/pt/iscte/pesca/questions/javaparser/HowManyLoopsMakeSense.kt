package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.Statement
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.getLoopControlStructures
import pt.iscte.pesca.extensions.hasLoopControlStructures
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.extensions.prettySignature
import pt.iscte.pesca.questions.subtypes.JavaParserQuestionRandomMethod
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.parsing.java.extensions.getOrNull
import kotlin.text.format

class HowManyLoopsMakeSense : JavaParserQuestionRandomMethod() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.body.getOrNull?.hasLoopControlStructures() == true

    override fun build(source: SourceCode, method: MethodDeclaration, language: Language): QuestionData {
        val loops = method.body.get().getLoopControlStructures()
        val howManyLoops = loops.size

        return QuestionData(
            source,
            TextWithCodeStatement(language["HowManyLoopsMakeSense"].format(method.nameAsString), method.prettySignature),
            howManyLoops.multipleChoice(language),
            language = language,
            relevantSourceCode = loops.map { SourceLocation(it as Statement) },
        )
    }
}