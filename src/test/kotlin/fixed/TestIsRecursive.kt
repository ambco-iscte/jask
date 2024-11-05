package fixed

import BaseTest
import org.junit.jupiter.api.Test
import pt.iscte.pesca.questions.fixed.*
import pt.iscte.pesca.questions.SimpleTextOption
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
            assertEquals(SimpleTextOption.yes(), it.solution.first())
        }

        assertIsApplicable(IsRecursive("hello")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption.no(), it.solution.first())
        }

        assertIsApplicable(IsRecursive("printHelloNTimes")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption.no(), it.solution.first())
        }

        assertIsApplicable(IsRecursive("printTimesN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption.no(), it.solution.first())
        }
    }
}