package compiler

import com.github.javaparser.StaticJavaParser
import org.junit.jupiter.api.Test
import pt.iscte.pesca.compiler.ErrorFinder
import kotlin.test.assertEquals

class TestFindUnknownType {

    @Test
    fun test() {
        val src = """
            class HelloWorld {
                private integer i = 0;
            
                public static int foo(nat n) {
                    return n;
                }
            }
        """.trimIndent()

        val errors = ErrorFinder(StaticJavaParser.parse(src)).findUnknownClasses()

        assertEquals(2, errors.size)

        val (e1, e2) = errors

        assertEquals("integer", e1.type.asString())
        assertEquals("nat", e2.type.asString())

        assertEquals(1, e1.types.size)
        assertEquals(1, e2.types.size)

        assertEquals("HelloWorld", e1.types.first().nameAsString)
        assertEquals("HelloWorld", e2.types.first().nameAsString)
    }
}