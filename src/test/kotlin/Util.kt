import org.junit.jupiter.api.Assertions.assertEquals
import pt.iscte.pesca.questions.QuestionData

fun QuestionData.assertUniqueSolution(solution: String) {
    assertEquals(1, this.solution.size, "Question must have a unique solution!\n$this")
    assertEquals(solution, this.solution.first().toString())
}