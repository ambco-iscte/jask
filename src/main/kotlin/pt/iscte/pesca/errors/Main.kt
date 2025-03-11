package pt.iscte.pesca.errors

import pt.iscte.pesca.Localisation
import pt.iscte.pesca.extensions.configureStaticJavaParser
import pt.iscte.pesca.questions.ReferencesUndefinedVariable
import pt.iscte.pesca.questions.compiler.AssignVarWithMethodWrongType
import pt.iscte.pesca.questions.compiler.CallMethodWithWrongParameterNumber
import pt.iscte.pesca.questions.compiler.CallMethodWithWrongParameterTypes
import pt.iscte.pesca.questions.compiler.MethodWithWrongReturnStmt
import pt.iscte.strudel.model.INT
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine

fun main() {
    val src = """
        class ArrayUtils {
            static int sum(int[] a) {
                int s = 0;
                for (int i = 0; i < a.length; i++)
                    s += a[i];
                return s;
            }
        }
    """.trimIndent()

    val module = Java2Strudel().load(src)

    val vm = IVirtualMachine.create()

    val sum = module.getProcedure("sum")
    val array = vm.allocateArrayOf(INT, 1, 2, 3, 4, 5)

    val (result, questions) = vm.executeWithQLC(sum, array)

    questions.forEachIndexed { i, sequence ->
        println("(${i + 1}) ${sequence.context}")
        println("==============================")
        sequence.questions.forEachIndexed { j, question ->
            println("(${j + 1}) $question\n")
        }
        println("==============================\n")
    }
}