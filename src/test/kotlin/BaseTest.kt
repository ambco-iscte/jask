import pt.iscte.pt.iscte.pesca.questions.Question
import pt.iscte.pt.iscte.pesca.questions.QuestionData
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertTrue

open class BaseTest(private val name: String) {
    private val source = File("src/test/resources/$name")

    fun assertIsApplicable(question: Question): QuestionData {
        assertTrue(question.isApplicable(source))
        return question.build(source)
    }

    fun assertIsNotApplicable(question: Question): QuestionData {
        assertFalse(question.isApplicable(source))
        return question.build(source)
    }
}