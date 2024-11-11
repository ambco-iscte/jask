package dynamic

import pt.iscte.pesca.questions.dynamic.*
import org.junit.jupiter.api.Test
import pt.iscte.pesca.extensions.pcall
import kotlin.test.assertEquals

class TestHowManyArrayReads {

    @Test
    fun test() {
        val src = """
            class Test {
                static boolean contains(int[] a, int n) {
                    for(int i = 0; i < a.length; i++)
                        if(a[i] == n)
                            return true;
                    return false;
                }
            }
        """.trimIndent()
        val qlc = HowManyArrayReads()

        val contains = qlc.generate(src, "contains".pcall(listOf(1, 2, 3, 4, 5), 3))
        assertEquals("3", contains.solution.first().toString())

        val notContains = qlc.generate(src, "contains".pcall(listOf(1, 2, 3, 4, 5), 6))
        assertEquals("5", notContains.solution.first().toString())

        val firstRead = qlc.generate(src, "contains".pcall(listOf(1, 2, 3, 4, 5), 1))
        assertEquals("1", firstRead.solution.first().toString())

        val emptyArray = qlc.generate(src, "contains".pcall(listOf<Int>(),4))
        assertEquals("0", emptyArray.solution.first().toString())
    }
}