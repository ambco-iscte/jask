import pt.iscte.pesca.questions.Question
import pt.iscte.pesca.questions.QuestionData
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

open class BaseTest(private val name: String) {
    private val source = File("src/test/resources/$name").readText()

    fun assertIsApplicable(question: Question): QuestionData {
        assertTrue(question.isApplicable(source))
        return question.generate(listOf(source))
    }

    fun assertIsNotApplicable(question: Question): QuestionData {
        assertFalse(question.isApplicable(source))
        return question.generate(listOf(source))
    }
}