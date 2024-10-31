package staticquestions

import BaseTest
import org.junit.jupiter.api.Test
import pt.iscte.pesca.questions.HowManyParams
import pt.iscte.pesca.questions.SimpleTextOption
import kotlin.test.assertEquals

class TestHowManyParameters : BaseTest("Example.java") {

    @Test
    fun test() {
        assertIsApplicable(HowManyParams("factorial")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(1), it.solution.first())
        }

        assertIsApplicable(HowManyParams("square")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(1), it.solution.first())
        }

        assertIsApplicable(HowManyParams("sum")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(2), it.solution.first())
        }

        assertIsApplicable(HowManyParams("hello")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(0), it.solution.first())
        }

        assertIsApplicable(HowManyParams("howManyPositiveEvensNumbersBeforeN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(1), it.solution.first())
        }

        assertIsApplicable(HowManyParams("printHelloNTimes")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(1), it.solution.first())
        }

        assertIsApplicable(HowManyParams("whileAndForMethod")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(0), it.solution.first())
        }

        assertIsApplicable(HowManyParams("printTimesN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(2), it.solution.first())
        }
    }
}