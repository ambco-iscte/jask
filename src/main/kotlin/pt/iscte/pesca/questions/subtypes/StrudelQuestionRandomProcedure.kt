package pt.iscte.pesca.questions.subtypes

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.toIValue
import pt.iscte.pesca.questions.DynamicQuestion
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SourceCodeWithInput
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

abstract class StrudelQuestionRandomProcedure : DynamicQuestion<IProcedure>() {

    override fun build(
        sources: List<SourceCodeWithInput>,
        language: Language
    ): QuestionData {
        val source = getApplicableSources<IProcedure>(sources).randomOrNull()
            ?: throw RuntimeException("Could not find an applicable source!")

        val module = Java2Strudel().load(source.source.code)
        val procedure = module.procedures.filterIsInstance<IProcedure>().filter {
            p -> isApplicable(p) && source.calls.any { it.id == p.id }
        }.randomOrNull() ?:
        throw RuntimeException(
            "Could not find applicable procedure in module ${module.id}!"
        )

        val vm = IVirtualMachine.create()
        setup(vm)

        val callsForProcedure = source.calls.filter { it.id == procedure.id }
        if (callsForProcedure.isEmpty())
            throw RuntimeException("Could not find procedure call specification for procedure ${procedure.id}.")
        val arguments = callsForProcedure.random().alternatives.random().map { it.toIValue(vm, module) }.toTypedArray()

        val call = "${procedure.id}(${arguments.joinToString()})"

        return build(vm, procedure, arguments.toList(), call, language)
    }

    open fun setup(vm: IVirtualMachine) { }

    protected abstract fun build(vm: IVirtualMachine, procedure: IProcedure, arguments: List<IValue>, call: String, language: Language): QuestionData
}