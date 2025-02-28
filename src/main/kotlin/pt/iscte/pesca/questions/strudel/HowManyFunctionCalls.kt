package pt.iscte.pesca.questions

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.getProcedureCalls
import pt.iscte.pesca.extensions.getUsedProceduresWithinModule
import pt.iscte.pesca.extensions.procedureCallAsString
import pt.iscte.pesca.extensions.sampleSequentially
import pt.iscte.pesca.extensions.toIValues
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IProcedureDeclaration
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

class HowManyFunctionCalls : DynamicQuestion<IProcedure>() {
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

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
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

        val distractors = sampleSequentially(3,
            count.values,
            listOf(
                count.values.sum(),
                count.values.sum() + 1,
                count.values.sum() - 1,
                correct + 1,
                correct - 1
            )
        ) { it != correct && it >= 0 }

        val options: MutableMap<Option, Boolean> =
            distractors.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(correct)] = true
        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return QuestionData(
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