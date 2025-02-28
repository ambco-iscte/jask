package compiler

import com.github.javaparser.StaticJavaParser
import org.junit.jupiter.api.Test
import pt.iscte.pesca.compiler.ErrorFinder
import kotlin.test.assertEquals

class TestFindUnknownMethod {

    @Test
    fun test() {
        val src = """
            class HelloWorld {
                public static int foo(int n) {
                    return 2 * bar(n);
                }
            }
        """.trimIndent()

        val errors = ErrorFinder(StaticJavaParser.parse(src)).findUnknownMethods()

        assertEquals(1, errors.size)

        val error = errors.first()
        assertEquals("bar", error.call.nameAsString)

        assertEquals(1, error.usable.size)
        assertEquals("foo", error.usable.first().nameAsString)
    }
}