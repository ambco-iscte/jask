package pt.iscte.jask.errors.compiler.templates

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.ReturnStmt
import pt.iscte.jask.Language
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.QuestionOption
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.MissingReturnInBranch
import pt.iscte.jask.extensions.nameWithScope
import pt.iscte.jask.extensions.simplify
import pt.iscte.jask.templates.StructuralQuestionTemplate
import pt.iscte.strudel.parsing.java.SourceLocation

class WhichReturnExecutesOnCondition(
    private val error: MissingReturnInBranch? = null
): StructuralQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean =
        if (error == null)
            CompilerErrorFinder(element.findCompilationUnit().get()).findMissingReturnInBranch().isNotEmpty()
        else
            element == error.method

    override fun build(
        sources: List<SourceCode>,
        language: Language
    ): Question {
        val (source, unit) = sources.getRandom<MethodDeclaration>()

        val error = this.error ?: CompilerErrorFinder(unit).findMissingReturnInBranch().random()

        val returns = error.method.findAll(ReturnStmt::class.java)

        val options: MutableMap<QuestionOption, Boolean> = returns.associate {
            SimpleTextOption(it) to false
        }.toMutableMap()
        options[SimpleTextOption.none(language, language["MissingReturnInBranch_Explained"].format())] = true

        return Question(
            source,
            TextWithCodeStatement(
                language["WhichReturnWhenCondition"].format(
                    "return",
                    error.method.nameWithScope(),
                    error.condition().simplify(false).toString()
                ),
                source.code
            ),
            options,
            language = language,
            relevantSourceCode = returns.map { SourceLocation(it) }
        )
    }
}