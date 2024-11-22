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
        val remaining = sources.toMutableList()
        var source: SourceCodeWithInput? = null
        while (source == null && remaining.isNotEmpty()) {
            val s = sources.random()
            source = s
            remaining.remove(s)
            runCatching { val module = Java2Strudel(checkJavaCompilation = false).load(s.source.code)
                if (module.procedures.filterIsInstance<IProcedure>().none {
                            p -> isApplicable(p) && s.calls.any { it.id == p.id }
                    })
                    source = null
            }.onFailure {
                source = null
            }
        }

        if (source == null)
            throw RuntimeException("Could not find applicable source for question type ${this::class.simpleName}!")

        val module = Java2Strudel(checkJavaCompilation = false).load(source!!.source.code)
        val procedure = module.procedures.filterIsInstance<IProcedure>().filter {
                p -> isApplicable(p) && source!!.calls.any { it.id == p.id }
        }.randomOrNull() ?:
        throw RuntimeException(
            "Could not find applicable procedure for question type ${this::class.simpleName} in module ${module.id}!"
        )

        val vm = IVirtualMachine.create()
        setup(vm)

        val callsForProcedure = source!!.calls.filter { it.id == procedure.id }
        if (callsForProcedure.isEmpty())
            throw RuntimeException("Could not find procedure call specification for procedure ${procedure.id}.")
        val randomCall = callsForProcedure.random()
        val arguments = randomCall.alternatives.random().map { it.toIValue(vm, module) }.toTypedArray()

        val x = callsForProcedure.filter { it != randomCall }.flatMap { it.alternatives }.map {
            a -> a.map { it.toIValue(vm, module) }
        }

        val call = "${procedure.id}(${arguments.joinToString()})"

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