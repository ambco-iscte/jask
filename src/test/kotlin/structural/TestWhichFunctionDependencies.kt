package structural

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.structural.WhichFunctionDependencies

class TestWhichFunctionDependencies {

    @Test
    fun testApplicable() {
        val src1 = """
            class Test {
                static void foo() { bar(); baz(); }
            }
        """.trimIndent()
        val qlc1 = assertDoesNotThrow { WhichFunctionDependencies().generate(src1) }
        println(qlc1)
        assertEquals(1, qlc1.solution.size)
        assertEquals("bar, baz", qlc1.solution[0].toString())

        val src2 = """
            class Test {
                static void foo() { bar(); }
            }
        """.trimIndent()
        val qlc2 = assertDoesNotThrow { WhichFunctionDependencies().generate(src2) }
        println(qlc2)
        assertEquals(1, qlc2.solution.size)
        assertEquals("bar", qlc2.solution[0].toString())

        val src3 = """
            class Test {
                static void foo() { foo(); bar(); }
            }
        """.trimIndent()
        val qlc3 = assertDoesNotThrow { WhichFunctionDependencies().generate(src3) }
        println(qlc3)
        assertEquals(1, qlc3.solution.size)
        assertEquals("bar", qlc3.solution[0].toString())
    }

    @Test
    fun testNotApplicable() {
        val src2 = """
            class Test {
                static void foo() { foo(); }
            }
        """.trimIndent()

        assertThrows<QuestionGenerationException> {
            WhichFunctionDependencies().generate(src2)
        }
    }
}