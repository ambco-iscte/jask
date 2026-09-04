package compiler

import com.github.javaparser.StaticJavaParser
import org.junit.jupiter.api.Test
import pt.iscte.jask.Localisation
import pt.iscte.jask.errors.CompilerErrorFinder
import pt.iscte.jask.errors.compiler.templates.WhichWrongReturnStmtTypeMethodReturnType
import kotlin.test.assertEquals

class TestMethodWithWrongReturnStmt {

    @Test
    fun test() {
        val src = """
            class HelloWorld {
                int foo() {
                    return "bar";
                }
            }
        """.trimIndent()

        val errors = CompilerErrorFinder(StaticJavaParser.parse(src)).findReturnStmtsWithWrongType()
        assertEquals(1, errors.size)

        val error = errors.single()
        println(error)

        assertEquals("foo", error.method.nameAsString)
        assertEquals("int", error.expected.asString())
        assertEquals("java.lang.String", error.actual.describe())

        // ---

        val qlc = WhichWrongReturnStmtTypeMethodReturnType()
        val data = qlc.generate(src, Localisation.getLanguage("en"))

        assertEquals(1, data.solution.size)
        assertEquals("int", data.solution.first().toString())
    }
}