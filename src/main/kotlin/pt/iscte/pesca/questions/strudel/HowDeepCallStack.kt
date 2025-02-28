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
import kotlin.math.max

class HowDeepCallStack : DynamicQuestion<IProcedure>() {

    var depth: Int = 0
    var numFunctionCalls: Int = 0

    override fun isApplicable(element: IProcedure): Boolean =
        element.getProcedureCalls().isNotEmpty()

    fun setup(vm: IVirtualMachine) {
        depth = 0
        numFunctionCalls = 0
        vm.addListener(object : IVirtualMachine.IListener {
            override fun procedureCall(procedure: IProcedureDeclaration, args: List<IValue>, caller: IProcedure?) {
                numFunctionCalls++
                depth = max(depth, vm.callStack.size)
            }
        })
    }

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        setup(vm)
        val arguments = args.toIValues(vm, module)

        vm.execute(procedure, *arguments.toTypedArray())

        val distractors = sampleSequentially(3, listOf(depth + 1, numFunctionCalls, numFunctionCalls + 1, 0)) {
            it != depth
        }

        val options: MutableMap<Option, Boolean> =
            distractors.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(depth)] = true
        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return QuestionData(
            source,
            TextWithCodeStatement(
                language["HowDeepCallStack"].format(procedureCallAsString(procedure, arguments)),
                listOf(procedure) + procedure.getUsedProceduresWithinModule()
            ),
            options,
            language = language
        )
    }
}