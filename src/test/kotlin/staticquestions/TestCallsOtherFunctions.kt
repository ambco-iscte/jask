package staticquestions

import BaseTest
import org.junit.jupiter.api.Test
import pt.iscte.pt.iscte.pesca.questions.NO
import pt.iscte.pt.iscte.pesca.questions.YES
import pt.iscte.pt.iscte.pesca.questions.CallsOtherFunctions
import kotlin.test.assertEquals

class TestCallsOtherFunctions : BaseTest("Example.java") {

    @Test
    fun test() {
        assertIsApplicable(CallsOtherFunctions("factorial")).let {
            assertEquals(1, it.solution.size)
            assertEquals(NO, it.solution.first())
        }

        assertIsApplicable(CallsOtherFunctions("square")).let {
            assertEquals(1, it.solution.size)
            assertEquals(NO, it.solution.first())
        }

        assertIsApplicable(CallsOtherFunctions("sum")).let {
            assertEquals(1, it.solution.size)
            assertEquals(NO, it.solution.first())
        }

        assertIsApplicable(CallsOtherFunctions("hello")).let {
            assertEquals(1, it.solution.size)
            assertEquals(YES, it.solution.first())
        }

        assertIsApplicable(CallsOtherFunctions("howManyPositiveEvensNumbersBeforeN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(NO, it.solution.first())
        }

        assertIsApplicable(CallsOtherFunctions("printHelloNTimes")).let {
            assertEquals(1, it.solution.size)
            assertEquals(YES, it.solution.first())
        }

        assertIsApplicable(CallsOtherFunctions("whileAndForMethod")).let {
            assertEquals(1, it.solution.size)
            assertEquals(NO, it.solution.first())
        }

        assertIsApplicable(CallsOtherFunctions("printTimesN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(YES, it.solution.first())
        }
    }
}