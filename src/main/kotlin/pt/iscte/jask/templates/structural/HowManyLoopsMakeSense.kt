package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.Statement
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getLoopControlStructures
import pt.iscte.jask.extensions.hasLoopControlStructures
import pt.iscte.jask.extensions.multipleChoice
import pt.iscte.jask.extensions.prettySignature
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