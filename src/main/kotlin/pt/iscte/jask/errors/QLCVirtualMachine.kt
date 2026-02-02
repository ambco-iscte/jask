package pt.iscte.jask.errors

import pt.iscte.jask.Language
import pt.iscte.jask.errors.runtime.toQLC
import pt.iscte.jask.extensions.toIValue
import pt.iscte.jask.common.QuestionSequenceWithContext
import pt.iscte.jask.common.SourceCode
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.ArrayIndexError
import pt.iscte.strudel.vm.DivisionByZeroError
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.LoopIterationLimitError
import pt.iscte.strudel.vm.NegativeArraySizeError
import pt.iscte.strudel.vm.NullReferenceError
import pt.iscte.strudel.vm.RuntimeError
import pt.iscte.strudel.vm.RuntimeErrorType
import pt.iscte.strudel.vm.StackOverflowError

data class QLCVirtualMachine(
    private val source: String,
    private val callStackMaximum: Int = 512,
    private val loopIterationMaximum: Int = 10000,
    private val availableMemory: Int = 1024,
    private val language: Language = Language.DEFAULT
) {
    fun execute(
        procedureID: String,
        vararg args: Any?
    ): Pair<IValue?, List<QuestionSequenceWithContext>> {
        val module = Java2Strudel().load(source)
        val procedure = module.getProcedure(procedureID) as IProcedure

        val vm = IVirtualMachine.create(callStackMaximum, loopIterationMaximum, availableMemory)
        val arguments = args.map { it.toIValue(vm, module) }

        val source = SourceCode(module.toString())

        val variableHistory = mutableMapOf<IVariableDeclaration<*>, List<IValue>>()

        val questions = mutableListOf<QuestionSequenceWithContext>()

        val listener = object : IVirtualMachine.IListener {
            override fun procedureCall(procedure: IProcedureDeclaration, args: List<IValue>, caller: IProcedure?) {
                procedure.parameters.forEachIndexed { index, parameter ->
                    if (parameter !in variableHistory)
                        variableHistory[parameter] = listOf(args[index])
                }
            }

            override fun variableAssignment(a: IVariableAssignment, value: IValue) {
                variableHistory[a.target] = (variableHistory[a.target] ?: emptyList()) + listOf(value)
            }

            override fun executionError(e: RuntimeError) {
                val question: QuestionSequenceWithContext? = try {
                    when (e.type) {
                        // Infinite Loop
                        RuntimeErrorType.LOOP_MAX ->
                            (e as LoopIterationLimitError).toQLC(source, procedure, arguments, variableHistory, language)

                        // Stack Overflow
                        RuntimeErrorType.STACK_OVERFLOW ->
                            (e as StackOverflowError).toQLC(source, procedure, arguments, language)

                        // Out of Memory
                        RuntimeErrorType.OUT_OF_MEMORY ->
                            null // TODO (e as OutOfMemoryError).toQLC()

                        // Division by Zero
                        RuntimeErrorType.DIVBYZERO ->
                            (e as DivisionByZeroError).toQLC(source, procedure, arguments, variableHistory, language)

                        // Non-initialised Variable
                        RuntimeErrorType.NONINIT_VARIABLE ->
                            null // TODO (e as UninitializedVariableError).toQLC()

                        // Null Pointer Exception
                        RuntimeErrorType.NULL_POINTER ->
                            (e as NullReferenceError).toQLC(source, procedure, arguments, variableHistory, language)

                        // Invalid Array Index
                        RuntimeErrorType.ARRAY_INDEX_BOUNDS ->
                            (e as ArrayIndexError).toQLC(source, procedure, arguments, variableHistory, language)

                        // Negative Array Size
                        RuntimeErrorType.NEGATIVE_ARRAY_SIZE ->
                            (e as NegativeArraySizeError).toQLC(source, procedure, arguments, variableHistory, language)

                        else -> {
                            e.printStackTrace()
                            null
                        }
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    null
                }

                if (question != null)
                    questions.add(question)
            }
        }

        vm.addListener(listener)
        val result = runCatching { vm.execute(procedure, *arguments.toTypedArray()) }.getOrNull()
        vm.removeListener(listener)

        return Pair(result, questions)
    }
}