package javaparser

import org.junit.jupiter.api.Test
import pt.iscte.pesca.Localisation
import pt.iscte.pesca.questions.HowManyVariables
import kotlin.test.assertEquals

class TestHowManyVariables {
    @Test
    fun test() {
        val src = """
            class Test {
                static double average(int a, int b) {
                    double n = 2.0;
                    return (a + b) / n;
                }           
            }
        """.trimIndent()

        val qlc = HowManyVariables().generate(src, Localisation.getLanguage("pt"))
        assertEquals("1", qlc.solution.first().toString())
    }
}