package structural

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.structural.WhichFunctionDependencies

class TestWhichFunctionDependencies {

    @Test
    fun testApplicable() {
        val src1 = """
            class Test {
                static void foo() { bar(); baz(); }
                
                static int bar() { return 42; }
                
                static int baz() { return 21; }
                
                static void hello() { }
                
                static void world() { }
            }
        """.trimIndent()
        val qlc1 = assertDoesNotThrow { WhichFunctionDependencies().generate(src1) }
        println(qlc1)
        assertEquals(1, qlc1.solution.size)
        assertEquals("bar, baz", qlc1.solution[0].toString())

        val src2 = """
            class Test {
                static void foo() { bar(); }
                
                static int bar() { return 42; }
            }
        """.trimIndent()
        val qlc2 = assertDoesNotThrow { WhichFunctionDependencies().generate(src2) }
        println(qlc2)
        assertEquals(1, qlc2.solution.size)
        assertEquals("bar", qlc2.solution[0].toString())

        val src3 = """
            class Test {
                static void foo() { foo(); bar(); }
                
                static int bar() { return 42; }
            }
        """.trimIndent()
        val qlc3 = assertDoesNotThrow { WhichFunctionDependencies().generate(src3) }
        println(qlc3)
        assertEquals(1, qlc3.solution.size)
        assertEquals("bar", qlc3.solution[0].toString())
    }

    @Test
    fun testPaddleBug() {
        val src = """
            class Test {
                static boolean isPerfectSquare(int n) {
                    double sqrt = Math.sqrt(n);
                    return sqrt == Math.floor(sqrt);
                }
            }
        """.trimIndent()

        val qlc = assertDoesNotThrow { WhichFunctionDependencies().generate(src) }
        println(qlc)

        assertEquals(1, qlc.solution.size)
        assertEquals("sqrt, floor", qlc.solution[0].toString())
    }

    @Test
    fun testPaddleExample() {
        val src = """
            class ArrayOrder {
                static void swap(int[] array, int i, int j) {
                    assert i >= 0 && i < array.length;
                    assert j >= 0 && j < array.length;
                
                    if(i != j) {
                        int t = array[i];
                        array[i] = array[j];
                        array[j] = t;
                    }
                }
                
                static void invert(int[] array) {
                    for(int i = 0; i < array.length / 2; i++)
                        swap(array, i, array.length - 1 - i);
                }
            }

            class RandomInts {
                static int random() {
                    return (int) (Math.random() * 1000000);
                }

                static int randomUntil(int max) {
                    assert max > 0;
                    return (int) (Math.random() * max);
                }

                static int randomWithin(int min, int max) {
                    assert min <= max;
                    return min + randomUntil(max - min + 1);
                }
            }

            class ArrayShuffle {
                static void randomSwap(int[] array) {
                    int i = RandomInts.randomUntil(array.length);
                    int j = RandomInts.randomUntil(array.length);
                    ArrayOrder.swap(array, i, j);
                }
                
                static void shuffle(int[] array) {
                    for(int i = array.length - 1; i > 0; i--) {
                        int r = RandomInts.randomWithin(0, i);
                        ArrayOrder.swap(array, r, i);
                    }
                }
            }
        """.trimIndent()

        val qlc = assertDoesNotThrow { WhichFunctionDependencies().generate(src) }
        println(qlc)
    }

    @Test
    fun testNotApplicable() {
        val src2 = """
            class Test {
                static void foo() { foo(); }
            }
        """.trimIndent()

        assertThrows<QuestionGenerationException> {
            WhichFunctionDependencies().generate(src2)
        }
    }
}