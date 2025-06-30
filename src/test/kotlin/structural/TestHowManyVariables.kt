package structural

import org.junit.jupiter.api.Test
import pt.iscte.jask.Localisation
import pt.iscte.jask.templates.structural.*
import pt.iscte.jask.templates.dynamic.*
import pt.iscte.jask.templates.quality.*
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

        val qlc = HowManyVariables().generate(src, Localisation.getLanguage("en"))
        assertEquals("1", qlc.solution.first().toString())
        println(qlc)
    }
}