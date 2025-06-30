package pt.iscte.jask.errors

import com.github.javaparser.StaticJavaParser
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine

fun main() {
    val src = """
        class Test {
            int x;
        
            static int foo(int a, int b) {
                int c = d;
                doubleval x = 3.14;
            }
        }
    """.trimIndent()

    val finder = CompilerErrorFinder(StaticJavaParser.parse(src))
    finder.findAllAndGenerateQLCs().forEach {
        println(it)
    }
}