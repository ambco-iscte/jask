package dynamic

import pt.iscte.pesca.Localisation
import pt.iscte.pesca.questions.ProcedureCall
import pt.iscte.pesca.questions.WhatIsResult
import pt.iscte.pesca.questions.WhichReturnExecuted
import kotlin.test.Test
import kotlin.test.assertEquals

class TestWhatIsResult {
    @Test
    fun test() {
        val src = """
            class Test {
                static double average(int a, int b) {
                    return (a +b)/2.0;
                }           
            }
        """.trimIndent()

        val qlc = WhatIsResult().generate(src, ProcedureCall("average", listOf(10, 11)), Localisation.getLanguage("pt"))
        assertEquals("10.5", qlc.solution.first().toString())

    }
}