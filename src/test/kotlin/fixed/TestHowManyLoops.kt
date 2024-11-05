package fixed

import BaseTest
import org.junit.jupiter.api.Test
import pt.iscte.pesca.questions.fixed.*
import pt.iscte.pesca.questions.SimpleTextOption
import kotlin.test.assertEquals

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
            assertEquals(SimpleTextOption("1"), it.solution.first())
        }

        assertIsApplicable(HowManyLoops("printHelloNTimes")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption("1"), it.solution.first())
        }

        assertIsApplicable(HowManyLoops("whileAndForMethod")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption("2"), it.solution.first())
        }
    }
}