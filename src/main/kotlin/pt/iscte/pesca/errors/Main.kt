package pt.iscte.pesca.errors

import pt.iscte.strudel.model.INT
import pt.iscte.strudel.model.IProcedure
import pt.iscte.strudel.parsing.java.Java2Strudel
import pt.iscte.strudel.vm.IVirtualMachine

fun main() {
    val src = """
        class ArrayUtils {
            static int sum(int[] a) {
                int s = 0;
                for (int i = 0; i <= a.length; i++)
                    s += a[i];
                return s;
            }
        }
    """.trimIndent()

    val module = Java2Strudel().load(src)

    val vm = IVirtualMachine.create()

    val sum = module.getProcedure("sum") as IProcedure
    val array = vm.allocateArrayOf(INT, 1, 2, 3, 4, 5)

    val (result, questions) = QLCVirtualMachine(vm).execute(sum, array)

    questions.forEach {
        println(it)
    }
}