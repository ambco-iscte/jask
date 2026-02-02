package pt.iscte.jask.errors.compiler.templates

import com.github.javaparser.ast.body.TypeDeclaration
import com.github.javaparser.ast.nodeTypes.NodeWithSimpleName
import pt.iscte.jask.Language
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.UnknownVariable
import pt.iscte.jask.extensions.randomBy
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.QuestionChoiceType
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.jask.templates.*
import kotlin.jvm.optionals.getOrNull

class WhichVariablesUsableAtLine(
    private val error: UnknownVariable? = null
): StructuralQuestionTemplate<TypeDeclaration<*>>() {

    init {
        if (error != null)
            require(error.scope.getUsableVariables().size >= 2)
    }

    override fun isApplicable(element: TypeDeclaration<*>): Boolean =
        if (error == null)
            CompilerErrorFinder(element).findUnknownVariables().any { it.scope.getUsableVariables().size >= 2 }
        else
            element.isAncestorOf(error.expr)

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): Question {
        val (source, unit) = sources.getRandom<TypeDeclaration<*>>()

        val errors = this.error?.let { listOf(it) } ?: CompilerErrorFinder(unit).findUnknownVariables()

        val (name, scope) = errors.randomBy { it.scope.getUsableVariables().size >= 2 }

        val line = name.range.get().begin.line
        val column = name.range.get().begin.column

        val usable: Set<NodeWithSimpleName<*>> = scope.getUsableVariables().filter {
            val l = it.nameAsExpression.range.getOrNull()?.begin?.line ?: return@filter false
            if (l == line) {
                val c = it.nameAsExpression.range.getOrNull()?.begin?.column ?: return@filter false
                c < column
            } else l < line
        }.toSet()
        val unusable: Set<NodeWithSimpleName<*>> = scope.enclosed.flatMap {
            it.getUsableVariables()
        }.minus(usable).toSet()

        return Question(
            source = source,
            statement = TextWithCodeStatement(
                language["WhichVariablesUsableAtLine"].format(line),
                source.code
            ),
            options = // TODO better distractors
                usable.associate { SimpleTextOption(it.nameAsString) to true } +
                        unusable.associate { SimpleTextOption(it.nameAsString) to false },
            language = language,
            choice = QuestionChoiceType.MULTIPLE,
            relevantSourceCode = listOf(SourceLocation(name))
        )
    }
}