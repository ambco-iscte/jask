package dynamic

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.extensions.procedureCallAsString
import pt.iscte.jask.templates.ProcedureCall
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.SourceCode
import pt.iscte.jask.templates.dynamic.HowManyArrayWrites
import pt.iscte.jask.templates.invoke
import pt.iscte.strudel.model.IArrayElementAssignment
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IArray
import pt.iscte.strudel.vm.IReference
import pt.iscte.strudel.vm.IValue
import pt.iscte.strudel.vm.IVirtualMachine
import kotlin.test.assertEquals

class TestHowManyArrayWrites {

    @Test
    fun testApplicable() {
        val src = """
            class Test {
                static void fill(int[] a, int e) {
                    for (int i = 0; i < a.length; i++) {
                        a[i] = e;
                    }
                }
            }
        """.trimIndent()

        assertDoesNotThrow {
            val qlc = HowManyArrayWrites().generate(src, "fill"(listOf(1, 2, 3, 4, 5), 42))
            assertEquals(1, qlc.solution.size)
            assertEquals("5", qlc.solution[0].toString())
        }

        assertDoesNotThrow {
            val qlc = HowManyArrayWrites().generate(src, "fill"(listOf(5, 4, 3, 2, 1), 42))
            assertEquals(1, qlc.solution.size)
            assertEquals("5", qlc.solution[0].toString())
        }
    }

    @Test
    fun testWeirdPaddleBugReplace() {
        val src = """
            class Test {
                static void replace(char[] letters, char find, char replace) {
                    for (int i = 0; i < letters.length; i++) {
                        if (letters[i] == find)
                            letters[i] = replace;
                    }
                }
            }
        """.trimIndent()

        val qlc = assertDoesNotThrow {
            HowManyArrayWrites().generate(src, "replace"(listOf('j', 'a', 'v', 'a'), 'e', 'i'))
        }
        println(qlc)

        assertEquals(1, qlc.solution.size)
        assertEquals("0", qlc.solution[0].toString())
    }

    @Test
    fun testWeirdPaddleBugConstrain() {
        val src = """
            class Test {
                static void constrain(double[] array, double min, double max) {
                    for(int i = 0; i < array.length; i++)
                        if(array[i] < min)
                            array[i] = min;
                        else if(array[i] > max)
                            array[i] = max; 
                }
            }
        """.trimIndent()

        val qlc1 = HowManyArrayWrites().generate(
            src,
            "constrain"(listOf(-1.3, 0.4, 1.4, 0.5), 0.0, 1.0)
        )
        println(qlc1)
        assertEquals(1, qlc1.solution.size)
        assertEquals("2", qlc1.solution[0].toString())


        val qlc2 = HowManyArrayWrites().generate(
            src,
            "constrain"(listOf(0.0, 0.4, 1.0, 0.5), 0.0, 1.0)
        )
        println(qlc2)
        assertEquals(1, qlc2.solution.size)
        assertEquals("0", qlc2.solution[0].toString())
    }

    @Test
    fun testNotApplicable() {
        val src = """
            class Test {
                static void fill(int[] a, int e) {
                    
                }
            }
        """.trimIndent()

        assertThrows<QuestionGenerationException> {
            HowManyArrayWrites().generate(src, "fill"(listOf(1, 2, 3, 4, 5), 42))
        }
    }

    @Test
    fun testMatrix() {
        val src = """
            class Test {
                static void fillMatrix() {
                    int[][] m = new int[2][3];
                    int n = 1;
                    for(int i = 0; i < m.length; i++)
                    for(int j = 0; j < m[i].length; j++) {
                        m[i][j] = n;
                        n++;
                    }
                }
            }
        """.trimIndent()

        val model = Java2Strudel().load(src)
        val vm = IVirtualMachine.create()
        vm.addListener(object : IVirtualMachine.IListener {
            override fun arrayElementAssignment(
                a: IArrayElementAssignment,
                ref: IReference<IArray>,
                index: Int,
                value: IValue
            ) {
                println(a)
            }
        })
        vm.execute(model.getProcedure("fillMatrix"))
        assertDoesNotThrow {
            val qlc = HowManyArrayWrites().generate(src, "fillMatrix"())
            println(qlc)
            assertEquals(1, qlc.solution.size)
            assertEquals("6", qlc.solution[0].toString())
        }
    }
}

fun main() {
    val src = """
            class Test {
                static void replaceLast(char[] letters, char find, char replace) {
                    for(int i = letters.length - 1 ; i >= 0; i--)
                        if(letters[i] == find) {
                            letters[i] = replace;
                            return;
                        }
                }
            }
        """.trimIndent()

    val module = Java2Strudel().load(src)
    val replace = module.getProcedure("replaceLast")

    val cases = listOf<ProcedureCall>(
        "replaceLast"(listOf('j', 'a', 'v', 'a'), 'a', 'i'),
        "replaceLast"(listOf<Char>(), 'a', 'i'),
        "replaceLast"(listOf('j', 'a', 'v', 'a'), 'j', 'c')
    )

    val frequency = cases.associateWith { 0 }.toMutableMap()

    repeat(10000) {
        val qlc = assertDoesNotThrow {
            HowManyArrayWrites().generate(SourceCode(src, cases))
        }
        cases.firstOrNull { procedureCallAsString(replace, it.arguments) in qlc.statement.statement }?.let {
            frequency[it] = (frequency[it] ?: 0) + 1
        }
    }

    frequency.entries.sortedBy { it.value }.forEach { (case, frequency) ->
        println("Chosen $frequency time(s):\t $case")
    }
}