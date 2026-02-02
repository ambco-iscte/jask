package pt.iscte.jask.errors.runtime

import pt.iscte.jask.Language
import pt.iscte.jask.common.QuestionSequenceWithContext
import pt.iscte.jask.common.SourceCode
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.LoopIterationLimitError

internal fun LoopIterationLimitError.toQLC(
    source: SourceCode,
    procedure: IProcedure,
    arguments: List<IValue>,
    variableHistory: Map<IVariableDeclaration<*>, List<IValue>>,
    language: Language
): QuestionSequenceWithContext {
    TODO("Not yet implemented: LoopIterationLimitError QLC")
}