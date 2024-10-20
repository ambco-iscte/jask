package staticquestions

import BaseTest
import org.junit.jupiter.api.Test
import pt.iscte.pt.iscte.pesca.questions.HowManyVariables
import pt.iscte.pt.iscte.pesca.questions.NO
import pt.iscte.pt.iscte.pesca.questions.SimpleTextOptionData
import kotlin.test.assertEquals

class TestHowManyVariables : BaseTest("Example.java") {

    @Test
    fun test() {
        assertIsApplicable(HowManyVariables("factorial")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOptionData(1), it.solution.first())
        }

        assertIsApplicable(HowManyVariables("square")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOptionData(0), it.solution.first())
        }

        assertIsApplicable(HowManyVariables("sum")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOptionData(0), it.solution.first())
        }

        assertIsApplicable(HowManyVariables("hello")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOptionData(1), it.solution.first())
        }

        assertIsApplicable(HowManyVariables("howManyPositiveEvensNumbersBeforeN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOptionData(2), it.solution.first())
        }

        assertIsApplicable(HowManyVariables("printHelloNTimes")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOptionData(0), it.solution.first())
        }

        assertIsApplicable(HowManyVariables("whileAndForMethod")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOptionData(2), it.solution.first())
        }

        assertIsApplicable(HowManyVariables("printTimesN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOptionData(0), it.solution.first())
        }
    }
}