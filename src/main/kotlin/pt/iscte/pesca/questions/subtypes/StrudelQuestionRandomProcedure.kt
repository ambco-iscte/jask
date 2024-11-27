package pt.iscte.pesca.questions.subtypes

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.toIValue
import pt.iscte.pesca.questions.Arguments
import pt.iscte.pesca.questions.DynamicQuestion
import pt.iscte.pesca.questions.ProcedureCall
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.QuestionGenerationException
import pt.iscte.pesca.questions.SourceCodeWithInput
import pt.iscte.strudel.model.IModule
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.extensions.hasThisParameter
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IRecord
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

abstract class StrudelQuestionRandomProcedure : DynamicQuestion<IProcedure>() {

    private fun getRandomApplicableProcedureAndArguments(
        vm: IVirtualMachine,
        module: IModule,
        calls: List<ProcedureCall>
    ): Pair<IProcedure, List<Any?>>? {
        val pairs = mutableListOf<Pair<IProcedure, List<Any?>>>()
        module.procedures.filterIsInstance<IProcedure>().forEach { p ->
            calls.forEach { call ->
                if (call.id == p.id) {
                    call.alternatives.forEach {
                        val args = it.map { it.toIValue(vm, module) }
                        if (isApplicable(p) && isApplicable(p, args))
                            pairs.add(p to it)
                    }
                }
            }
        }
        return pairs.randomOrNull()
    }

    // Pretty print :)
    private fun IValue.asString(): String = when (this@asString) {
        is IReference<*> -> target.asString()
        is IRecord -> "new $this"
        is IArray -> "[${elements.joinToString { it.asString() }}]"
        else -> toString()
    }

    private fun Collection<IValue>.joinAsString(): String = joinToString { it.asString() }

    override fun build(
        sources: List<SourceCodeWithInput>,
        language: Language
    ): QuestionData {
        val vm = IVirtualMachine.create()

        val source: SourceCodeWithInput? = sources.filter { s ->
            runCatching {
                val module = Java2Strudel(checkJavaCompilation = false).load(s.source.code)
                getRandomApplicableProcedureAndArguments(vm, module, s.calls) != null
            }.getOrDefault(false)
        }.randomOrNull()

        if (source == null)
            throw QuestionGenerationException(this, null, "Could not find source with at least one applicable procedure.")

        val module = Java2Strudel(checkJavaCompilation = false).load(source.source.code)

        val (procedure, args) = getRandomApplicableProcedureAndArguments(vm, module, source.calls) ?:
        throw QuestionGenerationException(this, source, "Could not find applicable procedure within source.")

        setup(vm)
        val arguments = args.map { it.toIValue(vm, module) }

        val call =
            if (procedure.hasThisParameter)
                "${arguments.first().asString()}.${procedure.id}(${arguments.subList(1, arguments.size).joinAsString()})"
            else
                "${procedure.id}(${arguments.joinAsString()})"

        return try {
            build(vm, procedure, arguments.toList(), call, language).apply {
                this.type = this@StrudelQuestionRandomProcedure::class.simpleName
                this.source = source
            }
        } catch (e: Exception) {
            throw RuntimeException("${this::class.simpleName}\n${e.stackTraceToString()}\n$procedure\nargs: ${arguments.toList()}")
        }
    }

    open fun setup(vm: IVirtualMachine) { }

    protected abstract fun build(
        vm: IVirtualMachine,
        procedure: IProcedure,
        arguments: List<IValue>,
        call: String,
        language: Language
    ): QuestionData
}