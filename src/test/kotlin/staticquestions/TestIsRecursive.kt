package staticquestions

import BaseTest
import org.junit.jupiter.api.Test
import pt.iscte.pt.iscte.pesca.questions.IsRecursive
import pt.iscte.pt.iscte.pesca.questions.NO
import pt.iscte.pt.iscte.pesca.questions.YES
import kotlin.test.assertEquals

class TestIsRecursive : BaseTest("Example.java") {

    @Test
    fun test() {
        assertIsNotApplicable(IsRecursive("square"))
        assertIsNotApplicable(IsRecursive("sum"))
        assertIsNotApplicable(IsRecursive("howManyPositiveEvensNumbersBeforeN"))
        assertIsNotApplicable(IsRecursive("whileAndForMethod"))

        assertIsApplicable(IsRecursive("factorial")).let {
            assertEquals(1, it.solution.size)
            assertEquals(YES, it.solution.first())
        }

        assertIsApplicable(IsRecursive("hello")).let {
            assertEquals(1, it.solution.size)
            assertEquals(NO, it.solution.first())
        }

        assertIsApplicable(IsRecursive("printHelloNTimes")).let {
            assertEquals(1, it.solution.size)
            assertEquals(NO, it.solution.first())
        }

        assertIsApplicable(IsRecursive("printTimesN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(NO, it.solution.first())
        }
    }
}