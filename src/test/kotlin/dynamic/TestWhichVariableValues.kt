package dynamic

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import pt.iscte.jask.templates.QuestionGenerationException
import pt.iscte.jask.templates.SourceCode
import pt.iscte.jask.templates.dynamic.WhichVariableValues
import pt.iscte.jask.templates.invoke

class TestWhichVariableValues {

    @Test
    fun testApplicable() {
        val src = """
            class Test {
                static int foo(int n) {
                    int m = n;
                    m = m + 1;
                    m = m + 2;
                    m = m + 3;
                    return m;
                }
            }
        """.trimIndent()

        assertDoesNotThrow {
            val qlc = WhichVariableValues().generate(src, "foo"(5))
            println(qlc)

            assertEquals(1, qlc.solution.size)
            assertEquals("5, 6, 8, 11", qlc.solution[0].toString())
        }
    }

    @Test
    fun testPaddleBugs() {
        val src = """
        class Divisors {
          static int countDivisors(int n) {
                int d = 1;
                int i = 1;
                while(i <= n / 2) {
                    if(n % i == 0) {
                        d = d + 1;
                    }
                    i = i + 1;
                }
                return d;
            }
            static int sumProperDivisors(int n) {
                int s = 0;
                int i = 1;
                while(i < n) {
                    if(n % i == 0) {
                        s = s + i;
                    }
                    i = i + 1;
                }
                return s;
            }
            static boolean isPrime(int n) {
                if(n == 1) {
                    return false;
                }
                int i = 2;
                while(i <= n / 2) {
                    if(n % i == 0) {
                        return false;
                    }
                    i = i + 1;
                }
                return true;
            }
        }
        
        class Primes {
            static int countPrimes(int max) {
                int i = 1;
                int c = 0;
                while(i <= max) {
                    if(Divisors.isPrime(i)) {
                        c = c + 1;
                    }
                    i = i + 1;
                }
                return c;
            }
            static boolean existsPrimeBetween(int min, int max) {
                int i = min + 1;
                while(i < max) {
                    if(Divisors.isPrime(i)) {
                        return true;
                    }
                    i = i + 1;
                }
                return false;
            }
        }
    """.trimIndent()

        val qlc1 = WhichVariableValues().generate(SourceCode(
            src,
            listOf("existsPrimeBetween"(5, 9))
        ))
        assertEquals(1, qlc1.solution.size)
        assertEquals("6, 7", qlc1.solution[0].toString())

        val qlc2 = WhichVariableValues().generate(SourceCode(
            src,
            listOf("existsPrimeBetween"(31, 37))
        ))
        assertEquals(1, qlc2.solution.size)
        assertEquals("32, 33, 34, 35, 36, 37", qlc2.solution[0].toString())
    }

    @Test
    fun testNotApplicable() {
        val src = """
            class Test {
                static int next(int n) {
                    return n + 1;
                }
            }
        """.trimIndent()

        assertThrows<QuestionGenerationException> {
            WhichVariableValues().generate(src, "next"(5))
        }
    }
}