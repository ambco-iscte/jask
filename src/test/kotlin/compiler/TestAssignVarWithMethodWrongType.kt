package compiler

import com.github.javaparser.StaticJavaParser
import pt.iscte.jask.Localisation
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.templates.WhichMethodCallReturnType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TestAssignVarWithMethodWrongType {

    @Test
    fun test() {
        val src = """
            class HelloWorld {
                int x = foo();
                
                String foo() {
                    return "bar";
                }
            }
        """.trimIndent()

        val errors = CompilerErrorFinder(StaticJavaParser.parse(src)).findVariablesAssignedWithWrongType()
        assertEquals(1, errors.size)

        val error = errors.single()
        println(error)

        assertTrue(error.initialiserIsMethodCall)
        assertEquals("int", error.expected.asString())
        assertEquals("java.lang.String", error.actual.describe())

        // ---

        val qlc = WhichMethodCallReturnType()
        val data = qlc.generate(src, Localisation.getLanguage("en"))

        assertEquals(1, data.solution.size)
        assertEquals("String", data.solution.first().toString())
    }
}