package structural

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import pt.iscte.jask.templates.structural.HowManyParams

class TestHowManyParams {

    @Test
    fun testApplicable() {
        val src1 = """
            class Test {
                static void foo() { int n = 42; }
            }
        """.trimIndent()
        val qlc1 = assertDoesNotThrow { HowManyParams().generate(src1) }
        println(qlc1)
        assertEquals(1, qlc1.solution.size)
        assertEquals("0", qlc1.solution[0].toString())

        val src2 = """
            class Test {
                static void foo(int n) { int n = 42; }
            }
        """.trimIndent()
        val qlc2 = assertDoesNotThrow { HowManyParams().generate(src2) }
        println(qlc2)
        assertEquals(1, qlc2.solution.size)
        assertEquals("1", qlc2.solution[0].toString())

        val src3 = """
            class Test {
                static void foo(int n, int m) { int n = 42; }
            }
        """.trimIndent()
        val qlc3 = assertDoesNotThrow { HowManyParams().generate(src3) }
        println(qlc3)
        assertEquals(1, qlc3.solution.size)
        assertEquals("2", qlc3.solution[0].toString())
    }
}