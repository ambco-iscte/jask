package pt.iscte.pesca.templates

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.Statement
import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.getLoopControlStructures
import pt.iscte.pesca.extensions.hasLoopControlStructures
import pt.iscte.pesca.extensions.multipleChoice
import pt.iscte.pesca.extensions.prettySignature
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.parsing.java.extensions.getOrNull

class HowManyLoopsMakeSense : StaticQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.body.getOrNull?.hasLoopControlStructures() == true

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val loops = method.body.get().getLoopControlStructures()
        val howManyLoops = loops.size

        return Question(
            source,
            TextWithCodeStatement(language["HowManyLoopsMakeSense"].format(method.nameAsString), method.prettySignature),
            howManyLoops.multipleChoice(language),
            language = language,
            relevantSourceCode = loops.map { SourceLocation(it as Statement) },
        )
    }
}