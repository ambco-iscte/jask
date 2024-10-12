package pt.iscte

import pt.iscte.pt.iscte.pesca.HowManyParameters
import pt.iscte.pt.iscte.pesca.IsRecursive
import pt.iscte.pt.iscte.pesca.build
import java.io.File

fun main() {
    val source = File("src/main/resources/HelloWorld.java")

    println(HowManyParameters("factorial").build(source))
    println()
    println(IsRecursive("factorial").build(source))
    println()

    println(HowManyParameters("square").build(source))
    println()
    println(IsRecursive("square").build(source))
    println()

    println(HowManyParameters("sum").build(source))
    println()
    println(IsRecursive("sum").build(source))
    println()

    println(HowManyParameters("hello").build(source))
    println()
    println(IsRecursive("hello").build(source))
}