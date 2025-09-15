package pt.iscte.jask.templates.dynamic
import pt.iscte.jask.templates.*

import pt.iscte.jask.Language
import pt.iscte.jask.extensions.countArrayAccesses
import pt.iscte.jask.extensions.getUsedProceduresWithinModule
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.extensions.toIValues
import pt.iscte.jask.extensions.toSetBy
import pt.iscte.strudel.model.IArrayLength
import pt.iscte.strudel.model.IBlock
import pt.iscte.strudel.model.IExpression
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine

class HowManyArrayReads : DynamicQuestionTemplate<IProcedure>() {
    var countReads = 0
    var countWrites = 0
    var len = 0
    val allocated = mutableListOf<Pair<IVariableDeclaration<*>, IArray>>()
    val reads = mutableListOf<String>()

    // There is at least one array access.
    override fun isApplicable(element: IProcedure): Boolean =
        element.countArrayAccesses() != 0

    fun setup(vm: IVirtualMachine) {
        countReads = 0
        countWrites = 0
        len = 0
        allocated.clear()
        reads.clear()
        vm.addListener(object : IVirtualMachine.IListener {
            override fun variableAssignment(a: IVariableAssignment, value: IValue) {
                if (value.type.isArrayReference)
                    allocated.add(a.target to (value as IReference<IArray>).target)
                else if (value.type.isArray)
                    allocated.add(a.target to value as IArray)
            }

            override fun arrayAllocated(ref: IReference<IArray>) {
                len += ref.target.length
                ref.target.addListener(object : IArray.IListener {
                    override fun elementRead(index: Int, value: IValue) {
                        countReads++
                        val arrayVar = allocated.firstOrNull { it.second == ref.target }?.first
                        if (arrayVar != null)
                            reads.add("${arrayVar.id}[$index]")
                    }

                    override fun elementChanged(index: Int, oldValue: IValue, newValue: IValue) {
                        countWrites++
                    }
                })
            }
        })
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
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

        val distractors: Set<Pair<Int, String?>> = sampleSequentially(3,
            listOf(
                allocated.size to language["HowManyArrayReads_DistractorNumAllocated"].format(allocated.joinToString { it.first.id!! }),
                countReads + 1 to null,
                countReads - 1 to null,
                countWrites to language["HowManyArrayReads_DistractorWrites"].format("a[i] = x", "x", "a", "i"),
                countWrites + 1 to null,
                countWrites - 1 to null,
                allocated.size + 1 to null,
                allocated.size - 1 to null,
                arrayLengthAccess to language["HowManyArrayReads_DistractorLengthAccesses"].format("length")
            ),
            listOf(
                len to language["HowManyArrayReads_DistractorLengthOfAllocated"].format(),
                len + 1 to null,
                len - 1 to null
            )
        ) {
            it.first != countReads && it.first >= 0
        }.toSetBy { it.first }

        val options: MutableMap<Option, Boolean> = distractors.associate {
            SimpleTextOption(it.first, it.second) to false
        }.toMutableMap()
        options[SimpleTextOption(
            countReads,
            language["HowManyArrayReads_Correct"].format("a", "i", "a[i]", reads.joinToString())
        )] = true
        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        val statement = language["HowManyArrayReads"].orAnonymous(arguments, procedure)
        return Question(
            source,
            TextWithCodeStatement(
                statement.format(procedureCallAsString(procedure, arguments)),
                listOf(procedure) + procedure.getUsedProceduresWithinModule()
            ),
            options,
            language = language
        )
    }
}