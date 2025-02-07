package pt.iscte.pesca.questions

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.correctAndRandomDistractors
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.TextWithCodeStatement
import pt.iscte.pesca.questions.subtypes.StrudelQuestionRandomProcedure
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IReturn
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

class WhichReturnExecuted : StrudelQuestionRandomProcedure() {

    override fun isApplicable(element: IProcedure): Boolean {
        return element.findAll(IReturn::class).size >= 2
    }

    override fun build(
        vm: IVirtualMachine,
        procedure: IProcedure,
        arguments: List<IValue>,
        call: String,
        language: Language
    ): QuestionData {
        var returnInst: IReturn? = null
        vm.addListener(object : IVirtualMachine.IListener {
            override fun returnCall(s: IReturn, returnValue: IValue?) {
                if (vm.callStack.size == 1) // first call
                    returnInst = s
            }
        })

        vm.execute(procedure, *arguments.toTypedArray())

        val line = returnInst?.getProperty(SourceLocation::class.java)?.startLine
                ?: throw RuntimeException("return line not found")

        val distractors = procedure.findAll(IReturn::class).mapNotNull {
            val l = it.getProperty(SourceLocation::class.java)?.startLine
            if(l != line)
                l
            else
                null
        }

        return QuestionData(
            TextWithCodeStatement(
                language[this::class.simpleName!!].format(
                    call
                ), procedure
            ),
            correctAndRandomDistractors(
                language["Line"] + " " + line,
                distractors.map {
                    language["Line"] + " " + it
                }.toSet()),
            language = language
        )
    }
}
