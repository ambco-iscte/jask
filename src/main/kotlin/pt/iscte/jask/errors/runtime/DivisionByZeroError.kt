package pt.iscte.jask.errors.runtime

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.deepFindAll
import pt.iscte.jask.extensions.findAll
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.toSetOf
import pt.iscte.jask.common.QuestionOption
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.QuestionChoiceType
import pt.iscte.jask.common.QuestionSequenceWithContext
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.jask.templates.dynamic.WhichVariableValues
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.IVariableExpression
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.vm.DivisionByZeroError
import pt.iscte.strudel.vm.IValue

fun DivisionByZeroError.toQLC(
    source: SourceCode,
    procedure: IProcedure,
    arguments: List<IValue>,
    variableHistory: Map<IVariableDeclaration<*>, List<IValue>>,
    language: Language
): QuestionSequenceWithContext {

    val procedureCallString = procedureCallAsString(procedure, arguments)

    fun whichVariablesUsedInDenominator(): Pair<Question, Set<IVariableDeclaration<*>>> {
        val used = this.exp.rightOperand.findAll<IVariableExpression>().toSetOf { it.variable }
        val all = procedure.deepFindAll<IVariableExpression>().toSetOf { it.variable } + variableHistory.keys

        val options: Map<QuestionOption, Boolean> = all.associate {
            SimpleTextOption(it.id, null) to (it in used)
        }

        return Question(
            source,
            TextWithCodeStatement(
                language["WhichVariablesInExpression"].orAnonymous(arguments, procedure).format(
                    this.exp.rightOperand.toString(),
                ),
                procedure
            ),
            options,
            choice = QuestionChoiceType.MULTIPLE,
            language = language,
            relevantSourceCode = listOf(SourceLocation(this.exp))
        ) to used
    }

    val (qlc, variables) = whichVariablesUsedInDenominator()

    fun whichVariableValues(variable: IVariableDeclaration<*>): Question =
        Question(
            source,
            TextWithCodeStatement(
                language["WhichVariableValues"].orAnonymous(arguments, procedure).format(
                    variable.id,
                    procedureCallString
                ),
                procedure
            ),
            WhichVariableValues.options(variable, variableHistory[variable]!!, variableHistory, arguments, language),
            language = language,
            relevantSourceCode = procedure.findAll(IVariableAssignment::class).filter {
                it.target == variable
            }.map { SourceLocation(it) }
        )

    val questions = listOf(qlc) + variables.map { whichVariableValues(it) }

    val context = TextWithCodeStatement(
        language["DivisionByZero"].format(
            this.exp.toString(),
            procedureCallString
        ),
        procedure
    )

    return QuestionSequenceWithContext(context, questions, language["DivisionByZeroFeedback"].format())
}