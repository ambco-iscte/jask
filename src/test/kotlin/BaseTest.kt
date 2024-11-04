import com.github.javaparser.ast.Node
import pt.iscte.pesca.questions.DynamicQuestion
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SourceCode
import pt.iscte.pesca.questions.SourceCodeWithInput
import pt.iscte.pesca.questions.StaticQuestion
import pt.iscte.strudel.model.IProgramElement
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

open class BaseTest(private val name: String) {
    internal val staticSource = SourceCode(File(this::class.java.getResource("/$name")!!.path).readText())
    internal val dynamicSource = SourceCodeWithInput(staticSource, listOf()) // TODO: procedure calls

    // Static
    internal inline fun <reified T : Node> assertIsApplicable(question: StaticQuestion<T>): QuestionData {
        assertTrue(question.isApplicable(staticSource, T::class))
        return question.generate(listOf(staticSource))
    }

    internal inline fun <reified T : Node> assertIsNotApplicable(question: StaticQuestion<T>) =
        assertFalse(question.isApplicable(staticSource, T::class))


    // Dynamic
    internal inline fun <reified T : IProgramElement> assertIsApplicable(question: DynamicQuestion<T>): QuestionData {
        assertTrue(question.isApplicable(dynamicSource, T::class))
        return question.generate(listOf(dynamicSource))
    }

    internal inline fun <reified T : IProgramElement> assertIsNotApplicable(question: DynamicQuestion<T>) =
        assertFalse(question.isApplicable(dynamicSource, T::class))
}