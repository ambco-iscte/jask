package structural

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.structural.WhatVariables

class TestWhatVariables {

    @Test
    fun testApplicable() {
        val src1 = """
            class Test {
                static void foo() {
                    int x = 1;
                    x = x + 1;
                    x = x + 1;
                    x = 42;
                }
            }
        """.trimIndent()
        val qlc1 = assertDoesNotThrow { WhatVariables().generate(src1) }
        println(qlc1)
        assertEquals(1, qlc1.solution.size)
        assertEquals("x", qlc1.solution[0].toString())

        val src2 = """
            class Test {
                static void foo() {
                    int x = 1;
                    x = x + 1;
                    x = x + 1;
                    x = 42;
                    int y = 2;
                }
            }
        """.trimIndent()
        val qlc2 = assertDoesNotThrow { WhatVariables().generate(src2) }
        println(qlc2)
        assertEquals(1, qlc2.solution.size)
        assertEquals("x, y", qlc2.solution[0].toString())

        val src3 = """
            class Test {
                static void foo() {
                    int x = 1;
                    x = x + 1;
                    x = x + 1;
                    x = 42;
                    int y = 2;
                    int z = 3;
                }
            }
        """.trimIndent()
        val qlc3 = assertDoesNotThrow { WhatVariables().generate(src3) }
        println(qlc3)
        assertEquals(1, qlc3.solution.size)
        assertEquals("x, y, z", qlc3.solution[0].toString())
    }

    @Test
    fun testNotApplicable() {
        val src = """
            class Test {
                static void foo(int n, int m) {
                    
                }
            }
        """.trimIndent()
        assertThrows<QuestionGenerationException> {
            WhatVariables().generate(src)
        }
    }
}