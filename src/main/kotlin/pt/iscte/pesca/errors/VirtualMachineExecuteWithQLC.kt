package pt.iscte.pesca.errors

import pt.iscte.pesca.questions.QuestionSequenceWithContext
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.RuntimeError
import pt.iscte.strudel.vm.RuntimeErrorType

fun IVirtualMachine.executeWithQLC(
    procedure: IProcedureDeclaration,
    vararg arguments: IValue
): Pair<IValue?, List<QuestionSequenceWithContext>> {
    val questions = mutableListOf<QuestionSequenceWithContext>()

    val listener = object : IVirtualMachine.IListener {
        override fun executionError(e: RuntimeError) {
            when (e.type) {
                // Infinite Loop
                RuntimeErrorType.LOOP_MAX -> {

                }

                // Stack Overflow
                RuntimeErrorType.STACK_OVERFLOW -> {

                }

                // Out of Memory
                RuntimeErrorType.OUT_OF_MEMORY -> {

                }

                // Division by Zero
                RuntimeErrorType.DIVBYZERO -> {

                }

                // Non-initialised Variable
                RuntimeErrorType.NONINIT_VARIABLE -> {

                }

                // Null Pointer Exception
                RuntimeErrorType.NULL_POINTER -> {

                }

                // Invalid Array Index
                RuntimeErrorType.ARRAY_INDEX_BOUNDS -> {

                }

                // Negative Array Size
                RuntimeErrorType.NEGATIVE_ARRAY_SIZE -> {

                }

                else -> e.printStackTrace()
            }
        }
    }

    addListener(listener)
    val result = execute(procedure, *arguments)
    removeListener(listener)

    return Pair(result, questions)
}