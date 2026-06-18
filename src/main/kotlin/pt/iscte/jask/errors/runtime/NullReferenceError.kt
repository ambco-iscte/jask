package pt.iscte.jask.errors.runtime

import com.github.javaparser.ast.Node
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.lineRelativeTo
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.QuestionSequenceWithContext
import pt.iscte.jask.common.SimpleTextStatement
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.templates.dynamic.WhichVariableValues
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.IVariableExpression
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.parsing.java.JP
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.NullReferenceError

fun NullReferenceError.toQLC(
    source: SourceCode,
    procedure: IProcedure,
    arguments: List<IValue>,
    variableHistory: Map<IVariableDeclaration<*>, List<IValue>>,
    language: Language
): QuestionSequenceWithContext {
    require(targetReference is IVariableExpression) {
        "NullReferenceError QLC: target expression must be a variable expression!"
    }

    val variable = targetReference as IVariableExpression
    require((variableHistory[variable.variable]?.count { !it.isNull  } ?: 0) >= 1) {
        "NullReferenceError QLC: target variable must take at least 1 non-null value!"
    }

    val procedureCallString = procedureCallAsString(procedure, arguments)

    fun whichVariableValues(): Question = Question(
        type = "WhichVariableValues",
        source = source,
        statement = SimpleTextStatement(language["WhichVariableValues"].format((targetReference as IVariableExpression).variable.id, procedureCallString)),
        WhichVariableValues.options(
            (targetReference as IVariableExpression).variable,
            variableHistory[(targetReference as IVariableExpression).variable] ?: emptyList(),
            variableHistory,
            arguments.toList(),
            language
        ),
        language = language,
        relevantSourceCode = procedure.findAll(IVariableAssignment::class).filter {
            it.target == (targetReference as IVariableExpression).variable
        }.map { SourceLocation(it) }
    )

    return QuestionSequenceWithContext(
        SimpleTextStatement(language["NullReferenceError"].format(
            procedureCallString,
            (targetReference.getProperty(JP) as Node).lineRelativeTo(procedure.getProperty(JP) as Node),
            variable.variable.id,
            "null"
        )),
        listOf(whichVariableValues()),
        language["NullReferenceErrorFeedback"].format("length") // TODO betterify
    )
}