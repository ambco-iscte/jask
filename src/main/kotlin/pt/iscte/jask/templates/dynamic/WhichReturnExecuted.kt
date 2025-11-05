package pt.iscte.jask.templates.dynamic
import jdk.jfr.Description
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

    @Description("Procedure must contain at least 2 return statements")
    override fun isApplicable(element: IProcedure): Boolean =
        element.findAll(IReturn::class).size >= 2

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        val arguments = args.toIValues(vm, module)
        val callAsString = procedureCallAsString(procedure, args)

        val otherReturns = mutableListOf<IReturn>()
        var returnInst: IReturn? = null
        vm.addListener(object : IVirtualMachine.IListener {
            override fun returnCall(s: IReturn, returnValue: IValue?) {
                if (vm.callStack.size == 1) // first call
                    returnInst = s
                otherReturns.add(s)
            }
        })

        vm.execute(procedure, *arguments.toTypedArray())

        if (returnInst == null)
            throw RuntimeException("No return calls executed at call stack size 1")

        val exec = returnInst!!.getProperty(JP)?.toString() ?:
        throw RuntimeException("Return instruction not bound to JavaParser source")

        val allReturns = procedure.findAll(IReturn::class) + otherReturns
        val distractors: Map<String, String?> = allReturns.mapNotNull {
            val l = it.getProperty(JP)?.toString()
            if (l != exec && l != null) {
                if (it.ownerProcedure == procedure)
                    l to language["WhichReturnExecuted_DistractorRightProcedureWrongStmt"].format(procedure.id, callAsString)
                else
                    l to language["WhichReturnExecuted_DistractorWrongProcedure"].format(it.ownerProcedure.id, procedure.id)
            }
            else null
        }.toSet().toMap()

        return Question(
            source,
            TextWithCodeStatement(
                language[this::class.simpleName!!].format(callAsString),
                procedure
            ),
            correctAndRandomDistractors(
                exec to null,
                distractors.toMap(),
            ),
            language = language
        )
    }
}
