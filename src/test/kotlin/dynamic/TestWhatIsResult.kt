package dynamic

import pt.iscte.jask.Localisation
import pt.iscte.jask.templates.ProcedureCall
import pt.iscte.jask.templates.structural.*
import pt.iscte.jask.templates.dynamic.*
import pt.iscte.jask.templates.quality.*
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