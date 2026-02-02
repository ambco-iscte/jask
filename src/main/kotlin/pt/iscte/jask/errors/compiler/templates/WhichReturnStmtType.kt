package pt.iscte.jask.errors.compiler.templates

import com.github.javaparser.ast.stmt.Statement
import pt.iscte.jask.Language
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.WrongReturnStmtType
import pt.iscte.jask.extensions.findAllTypes
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.common.QuestionOption
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.templates.StructuralQuestionTemplate
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.strudel.parsing.java.SourceLocation

class WhichReturnStmtType(
    private val error: WrongReturnStmtType? = null
): StructuralQuestionTemplate<Statement>() {

    override fun isApplicable(element: Statement): Boolean =
        if (error == null)
            CompilerErrorFinder(element.findCompilationUnit().get()).findReturnStmtsWithWrongType().isNotEmpty()
        else
            element == error.returnStmt

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, unit) = sources.getRandom<Statement>()

        val error = this.error ?: CompilerErrorFinder(unit).findReturnStmtsWithWrongType().random()

        val distractors = sampleSequentially(3, unit.findAllTypes().map {
            it.second to null
        }) {
            it.first.toString() != error.actual.describe()
        }

        val options: MutableMap<QuestionOption, Boolean> = distractors.associate {
            SimpleTextOption(it.first, it.second) to false
        }.toMutableMap()
        options[SimpleTextOption(error.actual.describe())] = true

        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return Question(
            source,
            TextWithCodeStatement(
                language["WhichReturnStmtType"].format(error.returnStmt.toString(), error.returnStmt.range.get().begin.line),
                source.code
            ),
            options,
            language = language,
            relevantSourceCode = listOf(SourceLocation(error.returnStmt), SourceLocation(error.method.type))
        )
    }
}