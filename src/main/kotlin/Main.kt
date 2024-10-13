package pt.iscte

import pt.iscte.pt.iscte.pesca.*
import java.io.File

val source = File("src/main/resources/HelloWorld.java")

fun TryIsRecursive() {
    println(IsRecursive("factorial",source).build())
    println()

    println(IsRecursive("square",source).build())
    println()

    println(IsRecursive("sum",source).build())
    println()

    println(IsRecursive("hello",source).build())
    println()

    println(IsRecursive("printHelloNTimes",source).build())
    println()
}

fun TryHowManyParameters() {
    println(HowManyParameters("factorial",source).build())
    println()

    println(HowManyParameters("square",source).build())
    println()

    println(HowManyParameters("sum",source).build())
    println()

    println(HowManyParameters("hello",source).build())
    println()
}

fun TryHowManyVariables() {
    println(HowManyVariables("factorial",source).build())
    println()

    println(HowManyVariables("square",source).build())
    println()

    println(HowManyVariables("sum",source).build())
    println()

    println(HowManyVariables("hello",source).build())
    println()

    println(HowManyVariables("howManyPositiveEvensNumbersBeforeN",source).build())
    println()

    println(HowManyVariables("printHelloNTimes",source).build())
    println()
}

fun TryHowManyLoops() {
    println(HowManyLoops("factorial",source).build())
    println()

    println(HowManyLoops("howManyPositiveEvensNumbersBeforeN",source).build())
    println()

    println(HowManyLoops("printHelloNTimes",source).build())
    println()

    println(HowManyLoops("whileAndForMethod",source).build())
    println()
}

fun TryCallsOtherFunctions() {
    println(CallsOtherFunctions("factorial",source).build())
    println()

    println(CallsOtherFunctions("howManyPositiveEvensNumbersBeforeN",source).build())
    println()

    println(CallsOtherFunctions("printHelloNTimes",source).build())
    println()
}

//Throws NoSuchMethodException
fun TryNoMethodError() {
    println(CallsOtherFunctions("THIS_METHOD_DOESNT_EXISTS",source).build())
    println()
}

fun main(){
    TryIsRecursive()
    TryHowManyParameters()
    TryHowManyVariables()
    TryHowManyLoops()
    TryCallsOtherFunctions()
    //TryNoMethodError() //Throws NoSuchMethodException
}