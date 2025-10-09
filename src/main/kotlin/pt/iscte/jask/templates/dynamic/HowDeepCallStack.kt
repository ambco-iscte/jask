package pt.iscte.jask.templates.dynamic
import pt.iscte.jask.templates.*

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.getProcedureCalls
import pt.iscte.jask.extensions.getUsedProceduresWithinModule
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.extensions.toIValues
import pt.iscte.jask.extensions.toSetBy
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.math.max

class HowDeepCallStack : DynamicQuestionTemplate<IProcedure>() {

    var depth: Int = 0
    var numFunctionCalls: Int = 0
    val currentSequence = mutableListOf<ProcedureCall>()
    val sequences = mutableListOf<List<ProcedureCall>>()
    val functionCalls: MutableList<ProcedureCall> = mutableListOf()
    var previousCallStackSize = 0

    override fun isApplicable(element: IProcedure): Boolean =
        element.getProcedureCalls().isNotEmpty()

    fun setup(vm: IVirtualMachine) {
        depth = 0
        numFunctionCalls = 0
        previousCallStackSize = vm.callStack.size
        functionCalls.clear()
        sequences.clear()
        currentSequence.clear()

        vm.addListener(object : IVirtualMachine.IListener {
            override fun procedureCall(procedure: IProcedureDeclaration, args: List<IValue>, caller: IProcedure?) {
                numFunctionCalls++
                functionCalls.add(ProcedureCall(procedure.id, args))

                if (vm.callStack.size > previousCallStackSize) {
                    currentSequence.add(ProcedureCall(procedure.id, args))
                    previousCallStackSize = vm.callStack.size
                } else {
                    sequences.add(currentSequence.toList())
                    currentSequence.clear()
                    previousCallStackSize = 0
                }
                depth = max(depth, vm.callStack.size)
            }
        })
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        setup(vm)
        val arguments = args.toIValues(vm, module)

        vm.execute(procedure, *arguments.toTypedArray())
        if (currentSequence !in sequences)
            sequences.add(currentSequence.toList())

        val distractors: Set<Pair<Int, String?>> = sampleSequentially(3, listOf(
            depth + 2 to null,
            depth + 1 to null,
            depth - 1 to null,
            numFunctionCalls to language["HowDeepCallStack_DistractorNumFunctionCalls"].format(),
            numFunctionCalls + 2 to null,
            numFunctionCalls + 1 to null,
            numFunctionCalls - 1 to null,
            0 to null
        )) {
            it.first != depth && it.first >= 0
        }.toSetBy { it.first }

        val options: MutableMap<Option, Boolean> =
            distractors.associate { SimpleTextOption(it.first, it.second) to false }.toMutableMap()
        options[SimpleTextOption(
            depth,
            language["HowDeepCallStack_Correct"].format(sequences.maxBy { it.size }.joinToString())
        )] = true
        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return Question(
            source,
            TextWithCodeStatement(
                language["HowDeepCallStack"].format(procedureCallAsString(procedure, args)),
                listOf(procedure) + procedure.getUsedProceduresWithinModule()
            ),
            options,
            language = language
        )
    }
}