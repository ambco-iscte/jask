package dynamic

import pt.iscte.jask.Localisation
import pt.iscte.jask.templates.ProcedureCall
import pt.iscte.jask.templates.SimpleTextOption
import pt.iscte.jask.templates.structural.*
import pt.iscte.jask.templates.dynamic.*
import pt.iscte.jask.templates.quality.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestWhichLastVariableValues {
    @Test
    fun test() {
        val src = """
            class Test {
                static void main(int i) {
                    int oneDiv2 = 1 / 2;
                    int oneMod2 = 1 % 2;
                    int sevenDiv2 = 7 / 2;
                    int sevenMod2 = 7 % 2;
                }           
            }
        """.trimIndent()

        val qlc = WhichLastVariableValues().generate(src, ProcedureCall("main", listOf(1000)), Localisation.getLanguage("pt"))
        println(qlc)
        assertTrue(qlc.solution.size == 1)
        assertEquals("oneDiv2 = 0, oneMod2 = 1, sevenDiv2 = 3, sevenMod2 = 1", (qlc.solution.first() as SimpleTextOption).text)
    }
}