package staticquestions

import BaseTest
import org.junit.jupiter.api.Test
import pt.iscte.pt.iscte.pesca.questions.NONE_OF_THE_ABOVE
import pt.iscte.pt.iscte.pesca.questions.SimpleTextOptionData
import pt.iscte.pt.iscte.pesca.questions.WhatIsTheReturnType
import kotlin.test.assertEquals

class TestWhatIsTheReturnType : BaseTest("Example.java") {

    @Test
    fun test() {
        assertIsApplicable(WhatIsTheReturnType("factorial")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOptionData("int"), it.solution.first())
        }

        assertIsApplicable(WhatIsTheReturnType("square")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOptionData("int"), it.solution.first())
        }

        assertIsApplicable(WhatIsTheReturnType("sum")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOptionData("int"), it.solution.first())
        }

        assertIsApplicable(WhatIsTheReturnType("hello")).let {
            assertEquals(1, it.solution.size)
            assertEquals(NONE_OF_THE_ABOVE, it.solution.first())
        }

        assertIsApplicable(WhatIsTheReturnType("howManyPositiveEvensNumbersBeforeN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOptionData("int"), it.solution.first())
        }

        assertIsApplicable(WhatIsTheReturnType("printHelloNTimes")).let {
            assertEquals(1, it.solution.size)
            assertEquals(NONE_OF_THE_ABOVE, it.solution.first())
        }

        assertIsApplicable(WhatIsTheReturnType("whileAndForMethod")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOptionData("int"), it.solution.first())
        }

        assertIsApplicable(WhatIsTheReturnType("printTimesN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOptionData("String"), it.solution.first())
        }
    }
}