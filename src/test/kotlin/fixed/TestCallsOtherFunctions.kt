package fixed

import BaseTest
import org.junit.jupiter.api.Test
import pt.iscte.pesca.questions.fixed.*
import pt.iscte.pesca.questions.SimpleTextOption
import kotlin.test.assertEquals

class TestCallsOtherFunctions : BaseTest("Example.java") {

    @Test
    fun test() {
        assertIsApplicable(CallsOtherFunctions("factorial")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption.no(), it.solution.first())
        }

        assertIsApplicable(CallsOtherFunctions("square")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption.no(), it.solution.first())
        }

        assertIsApplicable(CallsOtherFunctions("sum")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption.no(), it.solution.first())
        }

        assertIsApplicable(CallsOtherFunctions("hello")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption.yes(), it.solution.first())
        }

        assertIsApplicable(CallsOtherFunctions("howManyPositiveEvensNumbersBeforeN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption.no(), it.solution.first())
        }

        assertIsApplicable(CallsOtherFunctions("printHelloNTimes")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption.yes(), it.solution.first())
        }

        assertIsApplicable(CallsOtherFunctions("whileAndForMethod")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption.no(), it.solution.first())
        }

        assertIsApplicable(CallsOtherFunctions("printTimesN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption.yes(), it.solution.first())
        }
    }
}