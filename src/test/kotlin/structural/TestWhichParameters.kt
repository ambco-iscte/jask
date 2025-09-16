package structural

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.structural.HowManyParams
import pt.iscte.jask.templates.structural.WhichParametersMultipleChoice
import pt.iscte.jask.templates.structural.WhichParametersSingleChoice

class TestWhichParameters {

    @Test
    fun testSingleChoice() {
        val src1 = """
            class Test {
                static void foo() { int n = 42; }
            }
        """.trimIndent()
        val qlc1 = assertDoesNotThrow { WhichParametersSingleChoice().generate(src1) }
        println(qlc1)
        assertEquals(1, qlc1.solution.size)
        assertEquals("None of the above.", qlc1.solution[0].toString())

        val src2 = """
            class Test {
                static void foo(int n) { int m = 42; }
            }
        """.trimIndent()
        val qlc2 = assertDoesNotThrow { WhichParametersSingleChoice().generate(src2) }
        println(qlc2)
        assertEquals(1, qlc2.solution.size)
        assertEquals("n", qlc2.solution[0].toString())

        val src3 = """
            class Test {
                static void foo(int n, int m) { int x = 42; }
            }
        """.trimIndent()
        val qlc3 = assertDoesNotThrow { WhichParametersSingleChoice().generate(src3) }
        println(qlc3)
        assertEquals(1, qlc3.solution.size)
        assertEquals("n, m", qlc3.solution[0].toString())
    }

    @Test
    fun testMultipleChoice() {
        val src1 = """
            class Test {
                static void foo() { int n = 42; }
            }
        """.trimIndent()
        val qlc1 = assertDoesNotThrow { WhichParametersMultipleChoice().generate(src1) }
        println(qlc1)
        assertEquals(1, qlc1.solution.size)
        assertEquals("None of the above.", qlc1.solution[0].toString())

        val src2 = """
            class Test {
                static void foo(int n) { int m = 42; }
            }
        """.trimIndent()
        val qlc2 = assertDoesNotThrow { WhichParametersMultipleChoice().generate(src2) }
        println(qlc2)
        assertEquals(setOf("n"), qlc2.solution.map { it.toString() }.toSet())

        val src3 = """
            class Test {
                static void foo(int n, int m) { int x = 42; }
            }
        """.trimIndent()
        val qlc3 = assertDoesNotThrow { WhichParametersMultipleChoice().generate(src3) }
        println(qlc3)
        assertEquals(setOf("n", "m"), qlc3.solution.map { it.toString() }.toSet())
    }

    @Test
    fun testNotApplicable() {
        val src = """
            class Test {
                static int foo() { return 1; }
            }
        """.trimIndent()
        assertThrows<QuestionGenerationException> { WhichParametersSingleChoice().generate(src) }
        assertThrows<QuestionGenerationException> { WhichParametersMultipleChoice().generate(src) }
    }
}