package pt.iscte.pt.iscte.pesca

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.body.MethodDeclaration
import java.lang.reflect.Method


//Supported Languages
val PORTUGUESE_LANGUAGE = "pt"
val ENGLISH_LANGUAGE = "en"
val DEFAULT_LANGUAGE = "pt"

//Options that are always below the others
val NONE_OF_THE_ABOVE_OPTION = SimpleTextOptionData(mutableMapOf( ENGLISH_LANGUAGE to "None of the above.", PORTUGUESE_LANGUAGE to  "Nenhuma das anteriores."))
val ALL_OF_THE_ABOVE_OPTION = SimpleTextOptionData(mutableMapOf( ENGLISH_LANGUAGE to "All of the above.", PORTUGUESE_LANGUAGE to "Todas as anteriores."))
val YES_OPTION = SimpleTextOptionData(mutableMapOf( ENGLISH_LANGUAGE to "Yes", PORTUGUESE_LANGUAGE to "Sim"))
val NO_OPTION = SimpleTextOptionData(mutableMapOf( ENGLISH_LANGUAGE to "No", PORTUGUESE_LANGUAGE to "Não"))
val LAST_UNSHUFFLED_OPTIONS: MutableList<OptionData> = mutableListOf<OptionData>(
    NONE_OF_THE_ABOVE_OPTION,
    ALL_OF_THE_ABOVE_OPTION,
    YES_OPTION,
    NO_OPTION
)

fun getNearValuesAndNoneOfTheAbove(correctValue: Int):Map<OptionData, Boolean>{
    return mutableMapOf(
        SimpleTextOptionData(correctValue) to true,
        SimpleTextOptionData(correctValue + 1) to false,
        SimpleTextOptionData(if (correctValue == 0) 2 else correctValue - 1) to false,
        NONE_OF_THE_ABOVE_OPTION to false
    )
}

fun getTrueOrFalse(correctValue: Boolean):Map<OptionData, Boolean>{
    return mapOf(
        YES_OPTION to correctValue,
        NO_OPTION to !correctValue
    )
}

fun getMethod(source: String, methodName: String): MethodDeclaration {
    val unit = StaticJavaParser.parse(source)

    val method = unit.findAll(MethodDeclaration::class.java).firstOrNull { it.nameAsString == methodName }
    if (method == null)
        throw NoSuchMethodException("Method not found: $methodName")
    return method
}



// A helper function to convert primitive Java types to their boxed equivalents
fun Class<*>.boxed(): Class<*> = when (this) {
    java.lang.Byte.TYPE -> java.lang.Byte::class.java
    java.lang.Short.TYPE -> java.lang.Short::class.java
    java.lang.Integer.TYPE -> java.lang.Integer::class.java
    java.lang.Long.TYPE -> java.lang.Long::class.java
    java.lang.Float.TYPE -> java.lang.Float::class.java
    java.lang.Double.TYPE -> java.lang.Double::class.java
    java.lang.Character.TYPE -> java.lang.Character::class.java
    java.lang.Boolean.TYPE -> java.lang.Boolean::class.java
    else -> this
}

fun canCallJavaMethodWithArgs(methodDeclaration: MethodDeclaration, args: List<Any>): Boolean {
    // Obter os tipos dos parâmetros como string qualificada (ex.: "java.lang.String", "int")
    val paramTypes = methodDeclaration.parameters.map { it.type.asString() }

    // Se o número de parâmetros não corresponder ao número de argumentos, retorna falso
    if (paramTypes.size != args.size) return false

    return paramTypes.zip(args).all { (paramType, arg) ->
        val argClass = arg::class.java.boxed().name  // Obter a versão boxed da classe do argumento

        // Comparar o tipo do parâmetro com a classe do argumento
        paramType == argClass || paramType == arg::class.java.simpleName ||
                (paramType == "int" && argClass == "java.lang.Integer") ||
                (paramType == "float" && argClass == "java.lang.Float") ||
                (paramType == "double" && argClass == "java.lang.Double") ||
                (paramType == "boolean" && argClass == "java.lang.Boolean") ||
                (paramType == "char" && argClass == "java.lang.Character")
        // Adiciona mais verificações para outros tipos primitivos se necessário
    }
}

fun formattedArg(arg: Any): String {
    return when (arg) {
        is String -> "\"$arg\""   // Adiciona aspas duplas se for uma String
        is Char -> "\'$arg\'"     // Adiciona aspas simples se for um Char
        else -> arg.toString()    // Deixa como está para outros tipos
    }
}

val PRIMITIVE_JAVA_TYPES_EXCLUDING_VOID = setOf(
    "int", "boolean", "byte", "char", "short", "long", "float", "double"
)

fun isMethodReturningObject(methodDeclaration: MethodDeclaration): Boolean {
    val returnType = methodDeclaration.type.asString()  // Get the return type as a string

    // Check if the return type is an array or a String
    return when {
        returnType in PRIMITIVE_JAVA_TYPES_EXCLUDING_VOID -> true
        returnType.endsWith("[]") -> true
        returnType == "String" -> true
        else -> false
    }
}


fun callMethodWithArgs(instance: Any?, methodName: String, args: List<Any>): Any? {
    val argClasses = args.map { it::class.java.boxed() }.toTypedArray()  // Get the classes of the arguments

    // Find the method by its name and argument types
    val method: Method = instance?.javaClass?.getMethod(methodName, *argClasses)
        ?: throw NoSuchMethodException("Method not found: $methodName with args: $argClasses")

    // Invoke the method with the given arguments
    return method.invoke(instance, *args.toTypedArray())
}