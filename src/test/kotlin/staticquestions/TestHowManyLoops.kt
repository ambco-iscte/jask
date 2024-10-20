package staticquestions

import BaseTest
import org.junit.jupiter.api.Test
import pt.iscte.pt.iscte.pesca.questions.HowManyLoops
import pt.iscte.pt.iscte.pesca.questions.SimpleTextOptionData
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TestHowManyLoops : BaseTest("Example.java") {

    @Test
    fun test() {
        assertIsNotApplicable(HowManyLoops("factorial"))
        assertIsNotApplicable(HowManyLoops("square"))
        assertIsNotApplicable(HowManyLoops("sum"))
        assertIsNotApplicable(HowManyLoops("hello"))
        assertIsNotApplicable(HowManyLoops("printTimesN"))

        assertIsApplicable(HowManyLoops("howManyPositiveEvensNumbersBeforeN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOptionData("1"), it.solution.first())
        }

        assertIsApplicable(HowManyLoops("printHelloNTimes")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOptionData("1"), it.solution.first())
        }

        assertIsApplicable(HowManyLoops("whileAndForMethod")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOptionData("2"), it.solution.first())
        }
    }
}