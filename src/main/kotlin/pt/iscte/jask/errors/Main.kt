package pt.iscte.jask.errors

import com.github.javaparser.StaticJavaParser

fun compilerExample() {
    val src = """
        class Test {
            int x;
        
            static int foo(int a, int b) {
                int c = d;
                doubleval x = 3.14;
            }
        }
    """.trimIndent()

    CompilerErrorFinder(StaticJavaParser.parse(src)).findAllAndGenerateQLCs().forEach {
        println(it.toString() + "\n")
    }
}

fun runtimeExample() {
    val src = """
        class Test {
            static int sum(int[] a) {
                int s = 0;
                for (int i = 0; i <= a.length; i++) {
                    s = s + a[i];
                }
                return s;
            }
        }
    """.trimIndent()

    val (result, questions) = QLCVirtualMachine(src).execute("sum", listOf(1,2,3,4,5))
    questions.forEach { println(it) }
}

fun main() {
    // compilerExample()
    runtimeExample()
}