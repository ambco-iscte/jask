package dynamic

import org.junit.jupiter.api.Test
import pt.iscte.jask.Localisation
import pt.iscte.jask.common.ProcedureCall
import pt.iscte.jask.templates.dynamic.*
import kotlin.test.assertEquals

class TestWhatArraySize {

    @Test
    fun test() {
        val src = """
            class Test {
                static int[] subArray(int[] array, int from, int to) {
	                int[] sub = new int[to - from + 1];
                    for(int i = 0; i < sub.length; i++)
                        sub[i] = array[from + i];
                    return sub;
                }
            }
        """.trimIndent()
        val qlc = WhatArraySize()
        val subArray = qlc.generate(
            src,
            ProcedureCall("subArray", listOf(listOf(1, 2, 3, 4, 5, 6), 2, 4)),
            Localisation.getLanguage("en")
        )
        println(subArray)
        assertEquals("3", subArray.solution.first().toString())
    }
}