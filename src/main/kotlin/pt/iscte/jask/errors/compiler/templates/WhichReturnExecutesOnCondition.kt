package pt.iscte.jask.errors.compiler.templates

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.ReturnStmt
import javassist.bytecode.analysis.ControlFlow
import pt.iscte.jask.Language
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.QuestionOption
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.MissingReturnInBranch
import pt.iscte.jask.extensions.ControlFlowGraph
import pt.iscte.jask.extensions.ControlFlowPath
import pt.iscte.jask.extensions.condition
import pt.iscte.jask.extensions.endsWith
import pt.iscte.jask.extensions.equalsBoolean
import pt.iscte.jask.extensions.nameWithScope
import pt.iscte.jask.extensions.simplify
import pt.iscte.jask.templates.StructuralQuestionTemplate
import pt.iscte.strudel.parsing.java.SourceLocation

class WhichReturnExecutesOnCondition(
    private val error: MissingReturnInBranch? = null,
    private val branch: ControlFlowPath? = null
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
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val error = this.error ?: CompilerErrorFinder(method).findMissingReturnInBranch().random()
        val branch = this.branch ?: error.branch

        val valid = branch.endsWith<ControlFlowGraph.VirtualEndNode>()

        val returns = error.method.findAll(ReturnStmt::class.java)
        val branchReturn = (branch.firstOrNull {
            it.to is ControlFlowGraph.ReturnNode
        }?.to as? ControlFlowGraph.ReturnNode)?.bind

        if (!valid)
            require(branchReturn == null)

        val options: MutableMap<QuestionOption, Boolean> =
            returns.associate {
                SimpleTextOption(it) to (it === branchReturn)
            }.toMutableMap()

        options[SimpleTextOption.none(
            language,
            if (valid) null else language["MissingReturnInBranch_Explained"].format()
        )] = !valid

        return Question(
            source,
            TextWithCodeStatement(
                language["WhichReturnWhenCondition"].format(
                    "return",
                    error.method.nameWithScope(),
                    branch.condition().equalsBoolean(true).toString()
                ),
                source.code
            ),
            options,
            language = language,
            relevantSourceCode = returns.map { SourceLocation(it) }
        )
    }
}