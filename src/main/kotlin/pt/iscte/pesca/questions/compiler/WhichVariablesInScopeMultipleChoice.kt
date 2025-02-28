package pt.iscte.pesca.questions

import com.github.javaparser.ast.CompilationUnit
import pt.iscte.pesca.Language
import pt.iscte.pesca.compiler.ErrorFinder
import pt.iscte.strudel.parsing.java.SourceLocation

class WhichVariablesInScopeMultipleChoice : StaticQuestion<CompilationUnit>() {

    override fun isApplicable(element: CompilationUnit): Boolean =
        ErrorFinder(element).findUnknownVariables().any { it.scope.getUsableVariables().size >= 2 }

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): QuestionData {
        val (source, unit) = sources.getRandom<CompilationUnit>()

        val errors = ErrorFinder(unit).findUnknownVariables()

        val (name, scope) = errors.random()

        val usable: Set<String> = scope.getUsableVariables()
        val unusable: Set<String> = scope.enclosed.flatMap { it.getUsableVariables() }.minus(usable).toSet()

        return QuestionData(
            source = source,
            statement = TextWithCodeStatement(
                language["WhichVariablesInScope"].format(name.nameAsString, name.range.get().begin.line),
                source.code
            ),
            options = // TODO
                usable.associate { SimpleTextOption(it) to true } +
                unusable.associate { SimpleTextOption(it) to false },
            language = language,
            relevantSourceCode = listOf(SourceLocation(name))
        )
    }
}