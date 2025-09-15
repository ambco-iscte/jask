package pt.iscte.jask.templates.structural
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.Statement
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getBranches
import pt.iscte.jask.extensions.getLoopControlStructures
import pt.iscte.jask.extensions.hasLoopControlStructures
import pt.iscte.jask.extensions.multipleChoice
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.extensions.toSetBy
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.parsing.java.extensions.getOrNull

class HowManyLoops : StructuralQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.body.getOrNull?.hasLoopControlStructures() == true

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val loops = method.body.get().getLoopControlStructures()
        val howManyLoops = loops.size

        val howManyBranches = method.body.get().getBranches().size
        val howManyDistinctLoops = loops.map { it::class }.toSet().size

        val distractors = sampleSequentially(3, listOf(
            howManyBranches to language["HowManyLoops_DistractorBranches"].orAnonymous(method).format("if", method.nameAsString),
            howManyBranches + 1 to null,
            howManyBranches - 1 to null,
            howManyDistinctLoops to language["HowManyLoops_DistractorDistinctLoops"].orAnonymous(method).format("for, while"),
            howManyDistinctLoops + 1 to null,
            howManyDistinctLoops - 1 to null,
            howManyLoops + 1 to null,
            howManyLoops - 1 to null
        )) {
            it.first != howManyLoops && it.first > 0
        }.toSetBy { it.first }

        val options: MutableMap<Option, Boolean> = distractors.associate {
            SimpleTextOption(it.first, it.second) to false
        }.toMutableMap()

        options[SimpleTextOption(howManyLoops, null)] = true

        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return Question(
            source,
            TextWithCodeStatement(
                language["HowManyLoops"].orAnonymous(method).format(method.nameAsString),
                method
            ),
            options,
            language = language,
            relevantSourceCode = loops.map { SourceLocation(it as Statement) },
        )
    }
}