package pt.iscte.pesca.questions

import pt.iscte.pesca.Language
import pt.iscte.pesca.extensions.countArrayAccesses
import pt.iscte.pesca.extensions.getUsedProceduresWithinModule
import pt.iscte.pesca.extensions.procedureCallAsString
import pt.iscte.pesca.extensions.sampleSequentially
import pt.iscte.pesca.extensions.toIValues
import pt.iscte.strudel.model.IArrayLength
import pt.iscte.strudel.model.IBlock
import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

class HowManyArrayWrites : DynamicQuestion<IProcedure>() {

    var countWrites = 0
    var countReads = 0
    var len = 0
    var allocated  = 0

    // There is at least one array access.
    override fun isApplicable(element: IProcedure): Boolean =
        element.countArrayAccesses() != 0

    fun setup(vm: IVirtualMachine) {
        countWrites = 0
        countReads = 0
        len = 0
        allocated = 0
        vm.addListener(object : IVirtualMachine.IListener {
            override fun arrayAllocated(ref: IReference<IArray>) {
                allocated++
                len += ref.target.length
                ref.target.addListener(object : IArray.IListener {
                    override fun elementChanged(index: Int, oldValue: IValue, newValue: IValue) {
                        countWrites++
                    }

                    override fun elementRead(index: Int, value: IValue) {
                        countReads++
                    }
                })
            }
        })
    }

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val (source, module, procedure, args) = getRandomProcedure(sources)

        val vm = IVirtualMachine.create()
        setup(vm)
        val arguments = args.toIValues(vm, module)

        vm.execute(procedure, *arguments.toTypedArray())

        var arrayLengthAccess = 0
        procedure.accept(object : IBlock.IVisitor {
            override fun visitAny(exp: IExpression) {
                if (exp is IArrayLength)
                    arrayLengthAccess++
            }
        })

        val distractors = sampleSequentially(3,
            listOf(
                allocated,
                countReads,
                countReads + 1,
                countReads - 1,
                countWrites + 1,
                countWrites - 1,
                // countReads + countWrites
                allocated + 1,
                allocated - 1,
                arrayLengthAccess
            ),
            listOf(len, len + 1, len - 1)
        ) {
            it != countWrites && it >= 0
        }

        val options: MutableMap<Option, Boolean> = distractors.associate {
            SimpleTextOption(it) to false
        }.toMutableMap()
        options[SimpleTextOption(countWrites)] = true
        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return QuestionData(
            source,
            TextWithCodeStatement(
                language["HowManyArrayWrites"].format(procedureCallAsString(procedure, arguments)),
                listOf(procedure) + procedure.getUsedProceduresWithinModule()
            ),
            options,
            language = language
        )
    }
}