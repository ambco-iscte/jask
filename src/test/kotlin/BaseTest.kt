import com.github.javaparser.ast.Node
import pt.iscte.pesca.questions.Question
import pt.iscte.pesca.questions.QuestionData
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

open class BaseTest(private val name: String) {
    internal val source = File(this::class.java.getResource("/$name")!!.path).readText()

    internal inline fun <reified T : Node> assertIsApplicable(question: Question<T>): QuestionData {
        assertTrue(question.isApplicable<T>(source))
        return question.generate(listOf(source))
    }

    internal inline fun <reified T : Node> assertIsNotApplicable(question: Question<T>) =
        assertFalse(question.isApplicable<T>(source))
}