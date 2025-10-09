package pt.iscte.jask.errors.runtime

import pt.iscte.jask.Language
import pt.iscte.jask.errors.QLCVirtualMachine
import pt.iscte.jask.extensions.deepFindAll
import pt.iscte.jask.extensions.findAll
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.templates.Option
import pt.iscte.jask.templates.Question
import pt.iscte.jask.templates.QuestionChoiceType
import pt.iscte.jask.templates.QuestionSequenceWithContext
import pt.iscte.jask.templates.SimpleTextOption
import pt.iscte.jask.templates.SourceCode
import pt.iscte.jask.templates.TextWithCodeStatement
import pt.iscte.jask.templates.structural.IsRecursive
import pt.iscte.strudel.model.IBlock
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IProcedureCall
import pt.iscte.strudel.model.IReturn
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.StackOverflowError

fun StackOverflowError.toQLC(
    source: SourceCode,
    procedure: IProcedure,
    arguments: List<IValue>,
    language: Language
): QuestionSequenceWithContext {
    require(procedure.isRecursive) {
        "Cannot generate a StackOverflowError QLC for non-recursive procedure: ${procedure.id}"
    }

    val recursiveCalls = procedure.deepFindAll<IProcedureCall>().filter { call ->
        call.procedure == procedure
    }

    val baseCaseReturns = procedure.deepFindAll<IReturn>().filter { ret ->
        ret.expression?.findAll<IProcedureCall>()?.none { it.procedure == procedure } ?: true
    }

    val recursiveReturns = procedure.deepFindAll<IReturn>().minus(baseCaseReturns.toSet())

    val hasBaseCases = baseCaseReturns.isNotEmpty()

    // Is the function recursive?
    fun isProcedureRecursive(): Question = Question(
        type = "IsRecursive",
        source = source,
        statement = TextWithCodeStatement(language["IsRecursive"].format(procedure.id), procedure),
        options = IsRecursive.options(procedure.id!!, procedure.isRecursive, recursiveCalls.joinToString(), language),
        language = language,
        relevantSourceCode = recursiveCalls.map { SourceLocation(it) }
    )

    // Does the function have base cases?
    fun doesRecursionHaveBaseCases(): Question = Question(
        type = "DoesRecursionHaveBaseCases",
        source = source,
        statement = TextWithCodeStatement(language["HasRecursionBaseCases"].format(procedure.id), procedure),
        options = mapOf(
            SimpleTextOption.yes(language) to hasBaseCases,
            SimpleTextOption.no(language) to !hasBaseCases
        ),
        language = language,
        relevantSourceCode = baseCaseReturns.map { SourceLocation(it) }
    )

    // Which are the base cases of the function?
    fun whichAreTheBaseCases(): Question {
        require(hasBaseCases)

        val distractors = sampleSequentially(3,
            recursiveReturns,
            recursiveCalls
        ) {
            it !in baseCaseReturns
        }.toSet()

        val options: MutableMap<Option, Boolean> = distractors.associate {
            SimpleTextOption(it.toString().trim(), null) to false
        }.toMutableMap()

        baseCaseReturns.forEach {
            options[SimpleTextOption(it.toString().trim(), null)] = true
        }

        return Question(
            type = "WhichRecursionBaseCases",
            source = source,
            statement = TextWithCodeStatement(
                language["WhichRecursionBaseCases"].format(procedure.id),
                procedure
            ),
            options = options,
            language = language,
            choice = QuestionChoiceType.MULTIPLE,
            relevantSourceCode = baseCaseReturns.map { SourceLocation(it) }
        )
    }

    fun isBaseCaseReachable(): Question {
        require(hasBaseCases)
        TODO("Not yet implemented - Are the function's base cases reachable?") // How (undecidable?)
    }

    val context = TextWithCodeStatement(
        language["RecursionStackOverflow"].format(procedureCallAsString(procedure, arguments)),
        procedure
    )

    val sequence = mutableListOf(isProcedureRecursive())
    if (hasBaseCases) {
        sequence.add(whichAreTheBaseCases())
        // sequence.add(isBaseCaseReachable())
    } else {
        sequence.add(doesRecursionHaveBaseCases())
    }

    return QuestionSequenceWithContext(context, sequence)
}

fun main() {
    val src = """
        class Test {
            static int factorial(int n) {
                if (n == 0) {
                    return 1;
                } else {
                    return n * factorial(n);
                }
            }
        }
    """.trimIndent()

    val (result, questions) = QLCVirtualMachine(src).execute("factorial", 5)
    questions.forEach { println(it) }
}