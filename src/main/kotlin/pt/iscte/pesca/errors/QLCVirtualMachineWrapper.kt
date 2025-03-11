package pt.iscte.pesca.errors

import pt.iscte.pesca.questions.QuestionSequenceWithContext
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.RuntimeError
import pt.iscte.strudel.vm.RuntimeErrorType

class QLCVirtualMachineWrapper(
    val callStackMaximum: Int = 512,
    val loopIterationMaximum: Int = 10000,
    val availableMemory: Int = 1024
) {
    fun execute(procedure: IProcedure, vararg arguments: IValue): Pair<IValue?, List<QuestionSequenceWithContext>> =
        execute(procedure, arguments.toList())

    fun execute(procedure: IProcedure, arguments: List<IValue>): Pair<IValue?, List<QuestionSequenceWithContext>> {
        val vm = IVirtualMachine.create(callStackMaximum, loopIterationMaximum, availableMemory, true)
        val questions = mutableListOf<QuestionSequenceWithContext>()

        vm.addListener(object : IVirtualMachine.IListener {
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
        })

        val result = vm.execute(procedure, *arguments.toTypedArray())

        return Pair(result, questions)
    }
}