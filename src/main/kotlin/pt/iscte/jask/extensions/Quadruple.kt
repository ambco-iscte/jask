package pt.iscte.jask.extensions

import java.io.Serializable

class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
): Serializable {

    operator fun component1(): A = first

    operator fun component2(): B = second

    operator fun component3(): C = third

    operator fun component4(): D = fourth

    override fun toString(): String = "($first, $second, $third, $fourth)"
}