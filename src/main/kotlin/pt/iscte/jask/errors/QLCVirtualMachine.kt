package pt.iscte.jask.errors

import com.github.javaparser.ast.Node
import pt.iscte.jask.Language
import pt.iscte.jask.extensions.lineRelativeTo
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.extensions.toIValue
import pt.iscte.jask.templates.Option
import pt.iscte.jask.templates.QuestionChoiceType
import pt.iscte.jask.templates.Question
import pt.iscte.jask.templates.QuestionSequenceWithContext
import pt.iscte.jask.templates.SimpleTextOption
import pt.iscte.jask.templates.SimpleTextStatement
import pt.iscte.jask.templates.SourceCode
import pt.iscte.jask.templates.TextWithCodeStatement
import pt.iscte.jask.templates.dynamic.*
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.IVariableExpression
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.model.util.isIntLiteral
import pt.iscte.strudel.parsing.java.JP
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.vm.ArrayIndexError
import pt.iscte.strudel.vm.DivisionByZeroError
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import pt.iscte.strudel.vm.LoopIterationLimitError
import pt.iscte.strudel.vm.NullReferenceError
import pt.iscte.strudel.vm.RuntimeError
import pt.iscte.strudel.vm.RuntimeErrorType
import pt.iscte.strudel.vm.StackOverflowError
import pt.iscte.strudel.vm.UninitializedVariableError

data class QLCVirtualMachine(
    private val source: String,
    private val callStackMaximum: Int = 512,
    private val loopIterationMaximum: Int = 10000,
    private val availableMemory: Int = 1024,
    private val language: Language = Language.DEFAULT
) {
    fun execute(
        procedureID: String,
        vararg args: Any?
    ): Pair<IValue?, List<QuestionSequenceWithContext>> {
        val module = Java2Strudel().load(source)
        val procedure = module.getProcedure(procedureID) as IProcedure

        val vm = IVirtualMachine.create(callStackMaximum, loopIterationMaximum, availableMemory)
        val arguments = args.map { it.toIValue(vm, module) }

        val source = SourceCode(module.toString())
        val procedureCallString = procedureCallAsString(procedure, arguments.toList())

        val variableHistory = mutableMapOf<IVariableDeclaration<*>, List<IValue>>()

        val questions = mutableListOf<QuestionSequenceWithContext>()

        // Array Index Out of Bounds
        fun arrayIndexOutOfBounds(error: ArrayIndexError, indexIsVariableReference: Boolean) {
            val indexExpression = error.indexExpression as IVariableExpression
            val length = error.array.length

            val arrayDeclaration = (procedure.findAll(IVariableDeclaration::class) + procedure.parameters).first {
                error.target.isSame(it.expression())
            }

            // Which is the length of the array?
            fun whichArrayLength(): Question {
                val distractors = sampleSequentially(3, listOf(error.invalidIndex, error.array.elements.size, length - 1, length + 1, 0)) {
                    it != length
                }

                val options: MutableMap<Option, Boolean> =
                    distractors.associate { SimpleTextOption(it) to false }.toMutableMap()
                options[SimpleTextOption(length)] = true
                if (options.size < 4)
                    options[SimpleTextOption.none(language)] = false

                return Question(
                    type = "WhichLengthOfArray",
                    source = source,
                    statement = SimpleTextStatement(language["WhichLengthOfArray"].format(arrayDeclaration.id)),
                    options = options,
                    language = language,
                    choice = QuestionChoiceType.SINGLE,
                    relevantSourceCode = listOf(SourceLocation(arrayDeclaration))
                )
            }

            // Which is the last valid index of the array?
            fun whichLastArrayIndex(): Question {
                val lastIndex = length - 1

                val distractors = sampleSequentially(3, listOf(error.invalidIndex, error.array.elements.size, lastIndex - 1, length, length + 1, 0)) {
                    it != lastIndex
                }

                val options: MutableMap<Option, Boolean> =
                    distractors.associate { SimpleTextOption(it) to false }.toMutableMap()
                options[SimpleTextOption(lastIndex)] = true
                if (options.size < 4)
                    options[SimpleTextOption.none(language)] = false

                return Question(
                    type = "WhichLastValidArrayIndex",
                    source = source,
                    statement = SimpleTextStatement(language["WhichLastValidArrayIndex"].format(arrayDeclaration.id)),
                    options = options,
                    language = language,
                    choice = QuestionChoiceType.SINGLE,
                    relevantSourceCode = listOf(SourceLocation(arrayDeclaration))
                )
            }

            // Which variable is used to index the array?
            fun whichVariableUsedToIndex(): Question {
                val distractors = sampleSequentially(3, (procedure.localVariables + procedure.parameters).map { it.id }) {
                    it != indexExpression.toString()
                }

                val options: MutableMap<Option, Boolean> =
                    distractors.associate { SimpleTextOption(it) to false }.toMutableMap()
                options[SimpleTextOption(indexExpression)] = true
                if (options.size < 4)
                    options[SimpleTextOption.none(language)] = false

                return Question(
                    type = "WhichVariableUsedToIndexArray",
                    source = source,
                    statement = SimpleTextStatement(language["WhichVariableUsedToIndexArray"].format(arrayDeclaration.id)),
                    options = options,
                    language = language,
                    choice = QuestionChoiceType.SINGLE,
                    relevantSourceCode = listOf(SourceLocation(error.indexExpression))
                )
            }

            // Which values are taken by the variable?
            fun whichVariableValues(): Question = Question(
                type = "WhichVariableValues",
                source = source,
                statement = SimpleTextStatement(language["WhichVariableValues"].format(indexExpression.id, procedureCallString)),
                WhichVariableValues.options(
                    variableHistory[indexExpression.variable] ?: emptyList(),
                    variableHistory,
                    arguments.toList(),
                    language
                ),
                language = language,
                relevantSourceCode = procedure.findAll(IVariableAssignment::class).filter { it.target == indexExpression.variable }.map { SourceLocation(it) }
            )

            val context = TextWithCodeStatement(
                language["ArrayIndexOutOfBounds"].format(
                    procedureCallString,
                    (error.indexExpression.getProperty(JP) as Node).lineRelativeTo(procedure.getProperty(JP) as Node),
                    error.invalidIndex.toString(),
                    arrayDeclaration.id
                ),
                procedure
            )

            val seq = mutableListOf(whichArrayLength(), whichLastArrayIndex())
            if (indexIsVariableReference) {
                seq.add(whichVariableUsedToIndex())
                seq.add(whichVariableValues())
            }
            questions.add(QuestionSequenceWithContext(context, seq))
        }

        // Stack Overflow
        fun stackOverflow(error: StackOverflowError) {
            // Há casos base? (Obrigado Ruiva)
            // Se houver...
            //  Consigo chegar ao caso base?
            // Se não houver... concluo logo que nunca pode parar.

            fun isProcedureRecursive(): Question {
                TODO() // Does the function have recursive calls?
            }

            // Maybe: which are base cases?
            fun doesRecursionHaveBaseCases(): Question {
                TODO() // Is there a branch with a block which contains no recursive calls?
            }

            fun isBaseCaseReachable(): Question {
                TODO() // How
            }

            val context = TextWithCodeStatement(
                language["RecursionStackOverflow"].format(TODO()),
                procedure
            )

            questions.add(QuestionSequenceWithContext(context, listOf(
                isProcedureRecursive(),
                doesRecursionHaveBaseCases(),
                isBaseCaseReachable()
            )))
        }

        // Infinite Loop
        fun infiniteLoop(error: LoopIterationLimitError) {

        }

        // Null Reference
        fun nullReference(error: NullReferenceError) {

        }

        // Division by Zero
        fun divisionByZero(error: DivisionByZeroError) {

        }

        val listener = object : IVirtualMachine.IListener {
            override fun variableAssignment(a: IVariableAssignment, value: IValue) {
                variableHistory[a.target] = (variableHistory[a.target] ?: emptyList()) + listOf(value)
            }

            override fun executionError(e: RuntimeError) {
                when (e.type) {
                    // Infinite Loop
                    RuntimeErrorType.LOOP_MAX -> {
                        val error = e as LoopIterationLimitError
                        TODO()
                    }

                    // Stack Overflow
                    RuntimeErrorType.STACK_OVERFLOW -> {
                        stackOverflow(e as StackOverflowError)
                    }

                    // Out of Memory
                    RuntimeErrorType.OUT_OF_MEMORY -> {
                        TODO()
                    }

                    // Division by Zero
                    RuntimeErrorType.DIVBYZERO -> {
                        val error = e as DivisionByZeroError
                        TODO()
                    }

                    // Non-initialised Variable
                    RuntimeErrorType.NONINIT_VARIABLE -> {
                        val error = e as UninitializedVariableError
                        TODO()
                    }

                    // Null Pointer Exception
                    RuntimeErrorType.NULL_POINTER -> {
                        val error = e as NullReferenceError
                        TODO()
                    }

                    // Invalid Array Index
                    RuntimeErrorType.ARRAY_INDEX_BOUNDS -> {
                        val error = e as ArrayIndexError
                        if (error.indexExpression is IVariableExpression)
                            arrayIndexOutOfBounds(error, true)
                        else if (error.indexExpression.isIntLiteral())
                            arrayIndexOutOfBounds(error, false)
                    }

                    // Negative Array Size
                    RuntimeErrorType.NEGATIVE_ARRAY_SIZE -> {
                        TODO()
                    }

                    else -> e.printStackTrace()
                }
            }
        }

        vm.addListener(listener)
        val result = runCatching { vm.execute(procedure, *arguments.toTypedArray()) }.getOrNull()
        vm.removeListener(listener)

        return Pair(result, questions)
    }
}