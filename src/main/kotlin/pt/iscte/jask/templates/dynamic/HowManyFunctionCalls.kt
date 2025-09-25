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

class HowManyFunctionCalls : DynamicQuestionTemplate<IProcedure>() {

    val proceduresToConsider: MutableList<IProcedureDeclaration> = mutableListOf()
    val count: MutableMap<String, Int> = mutableMapOf()

    override fun isApplicable(element: IProcedure): Boolean =
        element.getProcedureCalls().isNotEmpty()

    fun setup(vm: IVirtualMachine) {
        proceduresToConsider.clear()
        count.clear()
        vm.addListener(object : IVirtualMachine.IListener {
            override fun procedureCall(procedure: IProcedureDeclaration, args: List<IValue>, caller: IProcedure?) {
                val p = procedure.id!!
                if (!vm.callStack.isEmpty && procedure in proceduresToConsider) {
                    count[p] = (count[p] ?: 0) + 1
                }
            }
        })
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        setup(vm)
        val arguments = args.toIValues(vm, module)

        proceduresToConsider.add(procedure)
        proceduresToConsider.addAll(procedure.getUsedProceduresWithinModule())

        vm.execute(procedure, *arguments.toTypedArray())

        val depProcedures = procedure.getProcedureCalls().map { it.procedure }.toSet()

        val randomProcedure = depProcedures.random()
        val correct = count[randomProcedure.id!!] ?: 0

        val distractors: Set<Pair<Int, String?>> = sampleSequentially(3,
            count.map { it.value to language["HowManyFunctionCalls_DistractorWrongProcedure"].format(it.key, randomProcedure.id) },
            listOf(
                count.values.sum() to language["HowManyFunctionCalls_DistractorTotalAllProcedures"].format(randomProcedure.id),
                count.values.sum() + 1 to null,
                count.values.sum() - 1 to null,
                correct + 1 to null,
                correct - 1 to null
            )
        ) { it.first != correct && it.first >= 0 }.toSetBy { it.first }

        val options: MutableMap<Option, Boolean> =
            distractors.associate { SimpleTextOption(it.first, it.second) to false }.toMutableMap()
        options[SimpleTextOption(correct)] = true
        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return Question(
            source,
            TextWithCodeStatement(
                language["HowManyFunctionCalls"].format(randomProcedure.id, procedureCallAsString(procedure, arguments)),
                listOf(procedure) + procedure.getUsedProceduresWithinModule()
            ),
            options,
            language
        )
    }
}