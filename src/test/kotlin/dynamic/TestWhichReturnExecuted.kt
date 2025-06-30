package dynamic

import pt.iscte.jask.Localisation
import pt.iscte.jask.templates.ProcedureCall
import pt.iscte.jask.templates.structural.*
import pt.iscte.jask.templates.dynamic.*
import pt.iscte.jask.templates.quality.*
import kotlin.test.Test
import kotlin.test.assertEquals

class TestWhichReturnExecuted {
    @Test
    fun test() {
        val src = """
            class Test {
            static double abs(double n) {
                if(n < 0) {
                    return -n;
                }
                else {
                    return n;
                }
            }
            }
        """.trimIndent()

        val qlc1 = WhichReturnExecuted().generate(src, ProcedureCall("abs", listOf(-2.0)), Localisation.getLanguage("pt"))
        assertEquals("Linha 4", qlc1.solution.first().toString())

        val qlc2 = WhichReturnExecuted().generate(src, ProcedureCall("abs", listOf(2.0)), Localisation.getLanguage("pt"))
        assertEquals("Linha 7", qlc2.solution.first().toString())
    }
}