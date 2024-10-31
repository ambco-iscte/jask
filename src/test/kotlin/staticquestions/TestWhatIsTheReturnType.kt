package staticquestions

import BaseTest
import org.junit.jupiter.api.Test
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.WhichReturnType
import kotlin.test.assertEquals

class TestWhatIsTheReturnType : BaseTest("Example.java") {

    @Test
    fun test() {
        assertIsApplicable(WhichReturnType("factorial")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption("int"), it.solution.first())
        }

        assertIsApplicable(WhichReturnType("square")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption("int"), it.solution.first())
        }

        assertIsApplicable(WhichReturnType("sum")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption("int"), it.solution.first())
        }

        assertIsApplicable(WhichReturnType("hello")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption("void"), it.solution.first())
        }

        assertIsApplicable(WhichReturnType("howManyPositiveEvensNumbersBeforeN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption("int"), it.solution.first())
        }

        assertIsApplicable(WhichReturnType("printHelloNTimes")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption("void"), it.solution.first())
        }

        assertIsApplicable(WhichReturnType("whileAndForMethod")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption("int"), it.solution.first())
        }

        assertIsApplicable(WhichReturnType("printTimesN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption("String"), it.solution.first())
        }
    }
}