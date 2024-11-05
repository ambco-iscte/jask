package fixed

import BaseTest
import org.junit.jupiter.api.Test
import pt.iscte.pesca.questions.fixed.*
import pt.iscte.pesca.questions.SimpleTextOption
import kotlin.test.assertEquals

class TestHowManyVariables : BaseTest("Example.java") {

    @Test
    fun test() {
        assertIsApplicable(HowManyVariables("factorial")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(1), it.solution.first())
        }

        assertIsApplicable(HowManyVariables("square")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(0), it.solution.first())
        }

        assertIsApplicable(HowManyVariables("sum")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(0), it.solution.first())
        }

        assertIsApplicable(HowManyVariables("hello")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(1), it.solution.first())
        }

        assertIsApplicable(HowManyVariables("howManyPositiveEvensNumbersBeforeN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(2), it.solution.first())
        }

        assertIsApplicable(HowManyVariables("printHelloNTimes")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(0), it.solution.first())
        }

        assertIsApplicable(HowManyVariables("whileAndForMethod")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(2), it.solution.first())
        }

        assertIsApplicable(HowManyVariables("printTimesN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(0), it.solution.first())
        }
    }
}