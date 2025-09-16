package structural

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.structural.HowManyFunctionDependencies

class TestHowManyFunctionDependencies {

    @Test
    fun testApplicable() {
        val src1 = """
            class Test {
                static void foo() { bar(); baz(); }
            }
        """.trimIndent()

        assertDoesNotThrow {
            val qlc = HowManyFunctionDependencies().generate(src1)
            println(qlc)

            assertEquals(1, qlc.solution.size)
            assertEquals("2", qlc.solution[0].toString())
        }

        val src2 = """
            class Test {
                static void foo() { bar(); }
            }
        """.trimIndent()

        assertDoesNotThrow {
            val qlc = HowManyFunctionDependencies().generate(src2)
            println(qlc)

            assertEquals(1, qlc.solution.size)
            assertEquals("1", qlc.solution[0].toString())
        }

        val src3 = """
            class Test {
                static void foo() {  }
            }
        """.trimIndent()

        assertDoesNotThrow {
            val qlc = HowManyFunctionDependencies().generate(src3)
            println(qlc)

            assertEquals(1, qlc.solution.size)
            assertEquals("0", qlc.solution[0].toString())
        }
    }

    @Test
    fun testNotApplicable() {
        val src2 = """
            class Test {
                
            }
        """.trimIndent()

        assertThrows<QuestionGenerationException> {
            HowManyFunctionDependencies().generate(src2)
        }
    }
}