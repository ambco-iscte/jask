package staticquestions

import BaseTest
import org.junit.jupiter.api.Test
import pt.iscte.pt.iscte.pesca.questions.NO
import pt.iscte.pt.iscte.pesca.questions.YES
import pt.iscte.pt.iscte.pesca.questions.CanCallAMethodWithGivenArguments
import kotlin.test.assertEquals

class TestCanCallAMethodWithGivenArguments : BaseTest("Example.java") {

    @Test
    fun testCanCall() {
        assertIsApplicable(CanCallAMethodWithGivenArguments("factorial", 5)).let {
            assertEquals(1, it.solution.size)
            assertEquals(YES, it.solution.first())
        }

        assertIsApplicable(CanCallAMethodWithGivenArguments("square", 5)).let {
            assertEquals(1, it.solution.size)
            assertEquals(YES, it.solution.first())
        }

        assertIsApplicable(CanCallAMethodWithGivenArguments("sum", 2, 3)).let {
            assertEquals(1, it.solution.size)
            assertEquals(YES, it.solution.first())
        }

        assertIsApplicable(CanCallAMethodWithGivenArguments("hello")).let {
            assertEquals(1, it.solution.size)
            assertEquals(YES, it.solution.first())
        }

        assertIsApplicable(CanCallAMethodWithGivenArguments("howManyPositiveEvensNumbersBeforeN", 5)).let {
            assertEquals(1, it.solution.size)
            assertEquals(YES, it.solution.first())
        }

        assertIsApplicable(CanCallAMethodWithGivenArguments("printHelloNTimes", 5)).let {
            assertEquals(1, it.solution.size)
            assertEquals(YES, it.solution.first())
        }

        assertIsApplicable(CanCallAMethodWithGivenArguments("whileAndForMethod")).let {
            assertEquals(1, it.solution.size)
            assertEquals(YES, it.solution.first())
        }

        assertIsApplicable(CanCallAMethodWithGivenArguments("printTimesN", "pesca is cool", 5)).let {
            assertEquals(1, it.solution.size)
            assertEquals(YES, it.solution.first())
        }
    }

    @Test
    fun testCannotCall() {
        assertIsApplicable(CanCallAMethodWithGivenArguments("factorial", 3.14)).let {
            assertEquals(1, it.solution.size)
            assertEquals(NO, it.solution.first())
        }

        assertIsApplicable(CanCallAMethodWithGivenArguments("square", 3.14)).let {
            assertEquals(1, it.solution.size)
            assertEquals(NO, it.solution.first())
        }

        assertIsApplicable(CanCallAMethodWithGivenArguments("sum", 3.14, 2.72)).let {
            assertEquals(1, it.solution.size)
            assertEquals(NO, it.solution.first())
        }

        assertIsApplicable(CanCallAMethodWithGivenArguments("hello", "goodbye")).let {
            assertEquals(1, it.solution.size)
            assertEquals(NO, it.solution.first())
        }

        assertIsApplicable(CanCallAMethodWithGivenArguments("howManyPositiveEvensNumbersBeforeN", 3.14)).let {
            assertEquals(1, it.solution.size)
            assertEquals(NO, it.solution.first())
        }

        assertIsApplicable(CanCallAMethodWithGivenArguments("printHelloNTimes", 3.14)).let {
            assertEquals(1, it.solution.size)
            assertEquals(NO, it.solution.first())
        }

        assertIsApplicable(CanCallAMethodWithGivenArguments("whileAndForMethod", "delicious strudel")).let {
            assertEquals(1, it.solution.size)
            assertEquals(NO, it.solution.first())
        }

        assertIsApplicable(CanCallAMethodWithGivenArguments("printTimesN", "pi", 3.14)).let {
            assertEquals(1, it.solution.size)
            assertEquals(NO, it.solution.first())
        }
    }
}