package pt.iscte

import pt.iscte.pt.iscte.pesca.*
import java.io.File

val source = File("src/main/resources/HelloWorld.java")

fun TryIsRecursive() {
    println(IsRecursive("factorial").build(source))
    println()

    println(IsRecursive("square").build(source))
    println()

    println(IsRecursive("sum").build(source))
    println()

    println(IsRecursive("hello").build(source))
    println()

    println(IsRecursive("printHelloNTimes").build(source))
    println()
}

fun TryHowManyVariables() {
    println(HowManyVariables("factorial").build(source))
    println()

    println(HowManyVariables("square").build(source))
    println()

    println(HowManyVariables("sum").build(source))
    println()

    println(HowManyVariables("hello").build(source))
    println()

    println(HowManyVariables("howManyPositiveEvensNumbersBeforeN").build(source))
    println()

    println(HowManyVariables("printHelloNTimes").build(source))
    println()
}

fun TryHowManyParameters() {
    println(HowManyParameters("factorial").build(source))
    println()

    println(HowManyParameters("square").build(source))
    println()

    println(HowManyParameters("sum").build(source))
    println()

    println(HowManyParameters("hello").build(source))
    println()
}

fun TryHowManyLoops() {
    println(HowManyLoops("factorial").build(source))
    println()

    println(HowManyLoops("howManyPositiveEvensNumbersBeforeN").build(source))
    println()

    println(HowManyLoops("printHelloNTimes").build(source))
    println()

    println(HowManyLoops("whileAndForMethod").build(source))
    println()
}

fun TryCallsOtherFunctions() {
    println(CallsOtherFunctions("factorial").build(source))
    println()

    println(CallsOtherFunctions("howManyPositiveEvensNumbersBeforeN").build(source))
    println()

    println(CallsOtherFunctions("printHelloNTimes").build(source))
    println()
}

fun main(){
    //TryIsRecursive()
    //TryHowManyVariables()
    //TryHowManyParameters()
    //TryHowManyLoops()
    //TryCallsOtherFunctions()
}