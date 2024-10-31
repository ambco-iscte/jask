package staticquestions

import BaseTest
import org.junit.jupiter.api.Test
import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SimpleTextOption
import pt.iscte.pesca.questions.WhatIsTheReturnType
import kotlin.test.assertEquals

class TestWhatIsTheReturnType : BaseTest("Example.java") {

    @Test
    fun test() {
        assertIsApplicable(WhatIsTheReturnType("factorial")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption("int"), it.solution.first())
        }

        assertIsApplicable(WhatIsTheReturnType("square")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption("int"), it.solution.first())
        }

        assertIsApplicable(WhatIsTheReturnType("sum")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption("int"), it.solution.first())
        }

        assertIsApplicable(WhatIsTheReturnType("hello")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption.none(), it.solution.first())
        }

        assertIsApplicable(WhatIsTheReturnType("howManyPositiveEvensNumbersBeforeN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption("int"), it.solution.first())
        }

        assertIsApplicable(WhatIsTheReturnType("printHelloNTimes")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption.none(), it.solution.first())
        }

        assertIsApplicable(WhatIsTheReturnType("whileAndForMethod")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption("int"), it.solution.first())
        }

        assertIsApplicable(WhatIsTheReturnType("printTimesN")).let {
            assertEquals(1, it.solution.size)
            assertEquals(SimpleTextOption("String"), it.solution.first())
        }
    }
}