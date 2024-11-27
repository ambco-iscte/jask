package pt.iscte.pesca.questions.subtypes

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.toIValue
import pt.iscte.pesca.questions.DynamicQuestion
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.QuestionGenerationException
import pt.iscte.pesca.questions.SourceCodeWithInput
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.extensions.hasThisParameter
import pt.iscte.strudel.vm.IRecord
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

abstract class StrudelQuestionRandomProcedure : DynamicQuestion<IProcedure>() {

    override fun build(
        sources: List<SourceCodeWithInput>,
        language: Language
    ): QuestionData {
        val source: SourceCodeWithInput? = sources.filter { s ->
            runCatching {
                val module = Java2Strudel(checkJavaCompilation = false).load(s.source.code)
                module.procedures.filterIsInstance<IProcedure>().any { procedure ->
                    isApplicable(procedure) && s.calls.any { it.id == procedure.id  }
                }
            }.getOrDefault(false)
        }.randomOrNull()

        if (source == null)
            throw QuestionGenerationException(this, null, "Could not find source with at least one applicable procedure.")

        val module = Java2Strudel(checkJavaCompilation = false).load(source.source.code)
        val procedure = module.procedures.filterIsInstance<IProcedure>().filter {
                p -> isApplicable(p) && source.calls.any { it.id == p.id }
        }.randomOrNull() ?:
        throw QuestionGenerationException(this, source, "Could not find applicable procedure within source.")

        val vm = IVirtualMachine.create()
        setup(vm)

        val callsForProcedure = source.calls.filter { it.id == procedure.id }
        if (callsForProcedure.isEmpty())
            throw QuestionGenerationException(this, source, "Could not find procedure call for chosen procedure.")

        val randomCall = callsForProcedure.random()
        val arguments = randomCall.alternatives.random().map { it.toIValue(vm, module) }

        val x = callsForProcedure.filter { it != randomCall }.flatMap { it.alternatives }.map {
            a -> a.map { it.toIValue(vm, module) }
        }

        // Pretty print :)
        fun IValue.asString(): String = when (this@asString) {
            is IReference<*> -> target.asString()
            is IRecord -> "new $this"
            else -> toString()
        }
        fun Collection<IValue>.joinAsString(): String = joinToString { it.asString() }

        val call =
            if (procedure.hasThisParameter)
                "${arguments.first().asString()}.${procedure.id}(${arguments.subList(1, arguments.size).joinAsString()})"
            else
                "${procedure.id}(${arguments.joinAsString()})"

        return build(vm, procedure, arguments.toList(), x, call, language)
    }

    open fun setup(vm: IVirtualMachine) { }

    protected abstract fun build(
        vm: IVirtualMachine,
        procedure: IProcedure,
        arguments: List<IValue>,
        alternatives: List<List<IValue>>,
        call: String,
        language: Language
    ): QuestionData
}