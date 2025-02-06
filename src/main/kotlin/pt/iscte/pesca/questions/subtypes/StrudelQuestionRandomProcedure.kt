package pt.iscte.pesca.questions.subtypes

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.toIValue
import pt.iscte.pesca.questions.DynamicQuestion
import pt.iscte.pesca.questions.ProcedureCall
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.QuestionGenerationException
import pt.iscte.pesca.questions.SourceCodeWithInput
import pt.iscte.strudel.model.IModule
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.parsing.java.CONSTRUCTOR_FLAG
import pt.iscte.strudel.parsing.java.JP
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.extensions.hasThisParameter
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IRecord
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

abstract class StrudelQuestionRandomProcedure : DynamicQuestion<IProcedure>() {

    private fun getRandomApplicableProcedureAndArguments(
        module: IModule,
        calls: List<ProcedureCall>
    ): Pair<IProcedure, List<Any?>>? {
        val vm = IVirtualMachine.create()
        val pairs = mutableListOf<Pair<IProcedure, List<Any?>>>()
        module.procedures.filterIsInstance<IProcedure>().filter { !it.hasFlag(CONSTRUCTOR_FLAG) }.forEach { p ->
            calls.forEach { call ->
                val args = call.arguments.map { it.toIValue(vm, module) }
                if (call.id == p.id && p.id != null) { // Specific test cases
                    call.arguments.forEach {
                        if (isApplicable(p) && isApplicable(p, args))
                            pairs.add(p to call.arguments)
                    }
                }
                else if (call.id == null) { // Wildcard test cases
                    runCatching { vm.execute(p, *args.toTypedArray()) }.onSuccess {
                        pairs.add(p to call.arguments)
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
        val source: SourceCodeWithInput? = sources.filter { s ->
            runCatching {
                val module = Java2Strudel(checkJavaCompilation = false).load(s.source.code)
                getRandomApplicableProcedureAndArguments(module, s.calls) != null
            }.getOrDefault(false)
        }.randomOrNull()

        if (source == null)
            throw QuestionGenerationException(this, null, "Could not find source with at least one applicable procedure.")

        val module = Java2Strudel(checkJavaCompilation = false).load(source.source.code)

        val (procedure, args) = getRandomApplicableProcedureAndArguments(module, source.calls) ?:
        throw QuestionGenerationException(this, source, "Could not find applicable procedure within source.")

        val vm = IVirtualMachine.create()
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