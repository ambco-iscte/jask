package staticquestions

import BaseTest
import org.junit.jupiter.api.Test
import pt.iscte.pesca.questions.HowManyParameters
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import kotlin.test.assertEquals

class TestHowManyParameters : BaseTest("Example.java") {

    @Test
    fun test() {
        assertIsApplicable(HowManyParameters("factorial")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(1), it.solution.first())
        }

        assertIsApplicable(HowManyParameters("square")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(1), it.solution.first())
        }

        assertIsApplicable(HowManyParameters("sum")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(2), it.solution.first())
        }

        assertIsApplicable(HowManyParameters("hello")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(0), it.solution.first())
        }

        assertIsApplicable(HowManyParameters("howManyPositiveEvensNumbersBeforeN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(1), it.solution.first())
        }

        assertIsApplicable(HowManyParameters("printHelloNTimes")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(1), it.solution.first())
        }

        assertIsApplicable(HowManyParameters("whileAndForMethod")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(0), it.solution.first())
        }

        assertIsApplicable(HowManyParameters("printTimesN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption(2), it.solution.first())
        }
    }
}