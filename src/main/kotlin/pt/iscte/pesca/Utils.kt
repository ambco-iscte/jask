package pt.iscte.pt.iscte.pesca

import com.github.javaparser.StaticJavaParser
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.nodeTypes.NodeWithBody
import com.github.javaparser.ast.stmt.DoStmt
import com.github.javaparser.ast.stmt.ForEachStmt
import com.github.javaparser.ast.stmt.ForStmt
import com.github.javaparser.ast.stmt.WhileStmt
import com.github.javaparser.ast.type.PrimitiveType
import pt.iscte.pt.iscte.pesca.questions.*
import java.lang.reflect.Method
import java.util.*

fun getNearValuesAndNoneOfTheAbove(correctValue: Int): Map<OptionData, Boolean>{
    return mutableMapOf(
        SimpleTextOptionData(correctValue) to true,
        SimpleTextOptionData(correctValue + 1) to false,
        SimpleTextOptionData(if (correctValue == 0) 2 else correctValue - 1) to false,
        NONE_OF_THE_ABOVE to false
    )
}

fun getTrueOrFalse(correctValue: Boolean):Map<OptionData, Boolean> =
    mapOf(YES to correctValue, NO to !correctValue)

fun getMethod(source: String, methodName: String): MethodDeclaration =
    StaticJavaParser.parse(source).findAll(MethodDeclaration::class.java).firstOrNull { it.nameAsString == methodName }
        ?: throw NoSuchMethodException("Method not found: $methodName")

val Class<*>.wrapper: Class<*>
    get() = this.kotlin.javaObjectType

val MethodDeclaration.prettySignature: String
    get() = "$typeAsString $nameAsString(${parameters.joinToString()})"

fun MethodDeclaration.accepts(arguments: List<Any>): Boolean {
    val paramTypes = this.parameters.map { it.type }
    if (paramTypes.size != arguments.size)
        return false
    return paramTypes.zip(arguments.map { it::class.java }).all { (parameterType, argumentType) ->
        val parameterTypeName =
            if (parameterType.isPrimitiveType) parameterType.asPrimitiveType().toBoxedType().nameAsString
            else parameterType.asString()
        parameterTypeName == argumentType.wrapper.name || parameterTypeName == argumentType.simpleName
    }
}

fun Node.getLoopControlStructures(): List<NodeWithBody<*>> =
    findAll(ForStmt::class.java) +
    findAll(DoStmt::class.java) +
    findAll(WhileStmt::class.java) +
    findAll(ForEachStmt::class.java)

val Any?.formatted: String
    get() = when (this) {
        is String -> "\"$this\""    // Strings are placed within "
        is Char -> "'$this'"        // Characters are placed within '
        else -> this.toString()     // Otherwise, value remains unchanged
    }

val JAVA_PRIMITIVE_TYPES: Set<String> =
    PrimitiveType.Primitive.values().map { it.name.lowercase(Locale.getDefault()) }.toSet()

fun MethodDeclaration.returnsPrimitiveOrArrayOrString(): Boolean =
    type.isPrimitiveType || type.isArrayType || type.toString() == String::class.simpleName

fun Any.call(methodName: String, arguments: List<Any>): Any? {
    val argumentTypes = arguments.map { it::class.java.wrapper }.toTypedArray()
    val method: Method = this.javaClass.getMethod(methodName, *argumentTypes)
        ?: throw NoSuchMethodException("Method not found: $methodName(${argumentTypes.joinToString { it.simpleName }})")
    return method.invoke(this, *arguments.toTypedArray())
}