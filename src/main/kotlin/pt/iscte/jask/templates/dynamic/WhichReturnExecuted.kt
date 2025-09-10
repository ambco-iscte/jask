package pt.iscte.jask.templates.dynamic
import pt.iscte.jask.templates.*

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.correctAndRandomDistractors
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.toIValues
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IReturn
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.parsing.java.JP
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

class WhichReturnExecuted : DynamicQuestionTemplate<IProcedure>() {

    override fun isApplicable(element: IProcedure): Boolean {
        return element.findAll(IReturn::class).size >= 2
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        val arguments = args.toIValues(vm, module)

        var returnInst: IReturn? = null
        vm.addListener(object : IVirtualMachine.IListener {
            override fun returnCall(s: IReturn, returnValue: IValue?) {
                if (vm.callStack.size == 1) // first call
                    returnInst = s
            }
        })

        vm.execute(procedure, *arguments.toTypedArray())

        val exec = returnInst?.getProperty(JP)?.toString() ?: throw RuntimeException("return source not found")

        val distractors = procedure.findAll(IReturn::class).mapNotNull {
            val l = it.getProperty(JP)?.toString()
            if(l != exec) l else null
        }

        return Question(
            source,
            TextWithCodeStatement(
                language[this::class.simpleName!!].format(procedureCallAsString(procedure, arguments)), procedure
            ),
            correctAndRandomDistractors(
                exec,
                distractors.toSet()
            ),
            language = language
        )
    }
}
