package pt.iscte.jask.errors.runtime

import com.github.javaparser.ast.Node
import pt.iscte.jask.Language
import pt.iscte.jask.Localisation
import pt.iscte.jask.errors.QLCVirtualMachine
import pt.iscte.jask.extensions.lineRelativeTo
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.extensions.sampleSequentially
import pt.iscte.jask.common.QuestionOption
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.QuestionChoiceType
import pt.iscte.jask.common.QuestionSequenceWithContext
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SimpleTextStatement
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithCodeStatement
import pt.iscte.jask.extensions.line
import pt.iscte.jask.templates.dynamic.WhichVariableValues
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.model.IVariableAssignment
import pt.iscte.strudel.model.IVariableDeclaration
import pt.iscte.strudel.model.IVariableExpression
import pt.iscte.strudel.model.util.findAll
import pt.iscte.strudel.parsing.java.JP
import pt.iscte.strudel.parsing.java.SourceLocation
import pt.iscte.strudel.vm.ArrayIndexError
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue

fun ArrayIndexError.toQLC(
    source: SourceCode,
    procedure: IProcedure,
    arguments: List<IValue>,
    variableHistory: Map<IVariableDeclaration<*>, List<IValue>>,
    language: Language
): QuestionSequenceWithContext {
    val indexIsVariableReference = this.indexExpression is IVariableExpression
    val indexExpression = this.indexExpression as? IVariableExpression
    val length = this.array.length

    val procedureCallString = procedureCallAsString(procedure, arguments)

    //val arrayDeclaration = (procedure.findAll(IVariableDeclaration::class) + procedure.parameters).first {
    //    this@toQLC.target.isSame(it.expression())
    //}

    val arrayDeclaration: IVariableDeclaration<*> = (
        procedure.findAll(IVariableDeclaration::class) + variableHistory.keys + procedure.parameters
    ).first {
        this@toQLC.target.isSame(it.expression())
    }

    val arrayDeclaringProcedure: IProcedure =
        arrayDeclaration.ownerProcedure

    // Which is the length of the array?
    fun whichArrayLength(): Question {
        val distractors = sampleSequentially(3, listOf(this.invalidIndex, this.array.elements.size, length - 1, length + 1, 0)) {
            it != length
        }

        val options: MutableMap<QuestionOption, Boolean> =
            distractors.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(length)] = true
        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        @Suppress("UNCHECKED_CAST")
        return Question(
            type = "WhichLengthOfArray",
            source = source,
            statement = SimpleTextStatement( language["WhichLengthOfArray"].format(
                "${arrayDeclaration.id}${(variableHistory[arrayDeclaration]?.first() as? IReference<IArray>)?.target?.elements?.let {
                    " ← [${it.joinToString()}]"
                } ?: ""}")
            ),
            options = options,
            language = language,
            choice = QuestionChoiceType.SINGLE,
            relevantSourceCode = listOf(SourceLocation(arrayDeclaration))
        )
    }

    // Which are the valid indices the array?
    fun whichAreTheValidIndices(): Question {
        val validIndices = (0 until length).toList()

        val distractors = sampleSequentially(3, listOf(
            (0 .. length).toList(),
            (1 until length).toList(),
            (1 .. length).toList()
        )) {
            it != validIndices
        }

        val options: MutableMap<QuestionOption, Boolean> =
            distractors.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(validIndices)] = true
        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return Question(
            type = "WhichValidArrayIndices",
            source = source,
            statement = SimpleTextStatement(language["WhichValidArrayIndices"].format(arrayDeclaration.id)),
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

        val options: MutableMap<QuestionOption, Boolean> =
            distractors.associate { SimpleTextOption(it) to false }.toMutableMap()
        options[SimpleTextOption(indexExpression)] = true
        if (options.size < 4)
            options[SimpleTextOption.none(language)] = false

        return Question(
            type = "WhichVariableUsedToIndexArray",
            source = source,
            statement = SimpleTextStatement(
                language["InsideTheFunction"].format(procedure.id!!) + ", " +  language["WhichVariableUsedToIndexArray"].format(arrayDeclaration.id).replaceFirstChar { it.lowercase() }),
            options = options,
            language = language,
            choice = QuestionChoiceType.SINGLE,
            relevantSourceCode = listOf(SourceLocation(this.indexExpression))
        )
    }

    // Which values are taken by the variable?
    fun whichVariableValues(): Question = Question(
        type = "WhichVariableValues",
        source = source,
        statement = SimpleTextStatement(
            language["WhichVariableValues"].format(indexExpression!!.id, procedureCallString)),
        WhichVariableValues.options(
            indexExpression.variable,
            variableHistory[indexExpression.variable] ?: emptyList(),
            variableHistory,
            arguments.toList(),
            language
        ),
        language = language,
        relevantSourceCode = procedure.findAll(IVariableAssignment::class).filter {
            it.target == indexExpression.variable
        }.map { SourceLocation(it) }
    )

    val context = TextWithCodeStatement(
        language["ArrayIndexOutOfBounds"].format(
            procedureCallString,
            (this.indexExpression.getProperty(JP) as Node).line,//.lineRelativeTo(procedure.getProperty(JP) as Node),
            this.invalidIndex.toString(),
            "${arrayDeclaration.id} → ${variableHistory[arrayDeclaration]?.firstOrNull() ?: arrayDeclaration.expression()}"
        ),
        procedure
    )

    val seq = mutableListOf(whichArrayLength(), whichAreTheValidIndices())
    if (indexIsVariableReference) {
        seq.add(whichVariableUsedToIndex())
        seq.add(whichVariableValues())
    }

    return QuestionSequenceWithContext(
        context,
        seq,
        language["ArrayIndexErrorFeedback"].format("n", "0", "n - 1")
    )
}

fun main() {
    val src = """
        class MyArrayExample {
            static double average(int[] arr) {
                int n = arr.length;
                double sum = 0.0;
                for (int i = 0; i <= arr.length; i++) {
                    sum = sum + arr[i];
                }
                return sum / n;
            }
            
            static double variance(int[] arr) {
                int n = arr.length;
                double avg = average(arr);
                double sum = 0.0;
                for (int i = 0; i < arr.length; i++) {
                    int x = arr[i];
                    sum = sum + (x - avg) * (x - avg);
                }
                return sum / n;
            }
            
            static void main() {
                int[] a = new int[] { 10, 16, 8, 27, 44, 34, 13 };
                double v = variance(a);
                System.out.println(v);
            }
        }
    """.trimIndent()

    val (_, questions) = QLCVirtualMachine(src).execute("main")
    questions.forEach { println(it) }
}