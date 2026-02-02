package pt.iscte.jask.errors.runtime

import com.github.javaparser.ast.Node
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.lineRelativeTo
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.common.QuestionOption
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.QuestionChoiceType
import pt.iscte.jask.common.QuestionSequenceWithContext
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SimpleTextStatement
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.jask.templates.dynamic.WhichVariableValues
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.IVariableExpression
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.parsing.java.JP
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.NegativeArraySizeError

fun NegativeArraySizeError.toQLC(
    source: SourceCode,
    procedure: IProcedure,
    arguments: List<IValue>,
    variableHistory: Map<IVariableDeclaration<*>, List<IValue>>,
    language: Language
): QuestionSequenceWithContext {
    require(this.lengthExpression is IVariableExpression) {
        "NegativeArraySizeError QLC: array allocation length expression must be a variable expression!"
    }

    val lengthExpression = this.lengthExpression as IVariableExpression
    require((variableHistory[lengthExpression.variable]?.size ?: 0) >= 2) {
        "NegativeArraySizeError QLC: variable used to set array length must take more than 1 value!"
    }

    val procedureCallString = procedureCallAsString(procedure, arguments)

    // Which variable is used to index the array?
    fun whichVariableUsedToIndex(): Question {
        val distractors = sampleSequentially(3, (procedure.localVariables + procedure.parameters).map { it.id }) {
            it != lengthExpression.toString()
        }

        val options: MutableMap<QuestionOption, Boolean> =
            distractors.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(lengthExpression)] = true
        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return Question(
            type = "WhichVariableUsedToIndexArray",
            source = source,
            statement = SimpleTextStatement(language["WhichVariableUsedToIndexArray"].format(this.allocation.id)),
            options = options,
            language = language,
            choice = QuestionChoiceType.SINGLE,
            relevantSourceCode = listOf(SourceLocation(this.lengthExpression))
        )
    }

    // Which values are taken by the variable?
    fun whichVariableValues(): Question = Question(
        type = "WhichVariableValues",
        source = source,
        statement = SimpleTextStatement(language["WhichVariableValues"].format(lengthExpression.id, procedureCallString)),
        WhichVariableValues.options(
            lengthExpression.variable,
            variableHistory[lengthExpression.variable] ?: emptyList(),
            variableHistory,
            arguments.toList(),
            language
        ),
        language = language,
        relevantSourceCode = procedure.findAll(IVariableAssignment::class).filter {
            it.target == lengthExpression.variable
        }.map { SourceLocation(it) }
    )

    val context = TextWithCodeStatement(
        language["NegativeArraySize"].format(
            procedureCallString,
            (this.lengthExpression.getProperty(JP) as Node).lineRelativeTo(procedure.getProperty(JP) as Node),
            this.allocation.id
        ),
        procedure
    )

    return QuestionSequenceWithContext(
        context,
        listOf(whichVariableUsedToIndex(), whichVariableValues()),
        language["NegativeArraySizeErrorFeedback"].format()
    )
}