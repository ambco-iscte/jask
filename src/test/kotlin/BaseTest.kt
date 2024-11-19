import com.github.javaparser.ast.Node
import pt.iscte.pesca.questions.DynamicQuestion
import pt.iscte.pesca.questions.ProcedureCall
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SourceCode
import pt.iscte.pesca.questions.SourceCodeWithInput
import pt.iscte.pesca.questions.StaticQuestion
import pt.iscte.strudel.model.IProgramElement
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal open class BaseStaticTest(private val source: String) {
    val staticSource = SourceCode(source)

    // Static
    inline fun <reified T : Node> assertIsApplicable(question: StaticQuestion<T>): QuestionData {
        assertTrue(question.isApplicable(staticSource, T::class))
        return question.generate(listOf(staticSource))
    }

    inline fun <reified T : Node> assertIsNotApplicable(question: StaticQuestion<T>) =
        assertFalse(question.isApplicable(staticSource, T::class))
}

internal open class BaseDynamicTest(private val source: String, private val calls: List<ProcedureCall>) {
    val dynamicSource = SourceCodeWithInput(SourceCode(source), calls) // TODO: procedure calls

    // Dynamic
    inline fun <reified T : IProgramElement> assertIsApplicable(question: DynamicQuestion<T>): QuestionData {
        assertTrue(question.isApplicable(dynamicSource, T::class))
        return question.generate(listOf(dynamicSource))
    }

    inline fun <reified T : IProgramElement> assertIsNotApplicable(question: DynamicQuestion<T>) =
        assertFalse(question.isApplicable(dynamicSource, T::class))
}