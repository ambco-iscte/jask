package pt.iscte.jask.templates.quality
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.body.VariableDeclarator
import com.github.javaparser.ast.expr.*
import com.github.javaparser.ast.stmt.*
import com.github.javaparser.ast.type.PrimitiveType
import pt.iscte.jask.Language
import pt.iscte.jask.Localisation
import pt.iscte.jask.extensions.*

public class ReplaceWhileWithFor  : StaticQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean {
        val list = element.findAll(WhileStmt::class.java)
        return list.isNotEmpty()
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()

        val whileLoop = methodReplaced.findAll(WhileStmt::class.java).random()

        val variables = method.findAll(VariableDeclarationExpr::class.java)
        var i = 0;
        val varNameOG = "var"
        var varName = varNameOG

        while (variables.any { variableDeclaration ->
                variableDeclaration.variables.any { it.nameAsString == varName }
            }){
            i++;
            varName = varNameOG + i.toString()
        }

        val init = VariableDeclarationExpr(
            VariableDeclarator(
                PrimitiveType(PrimitiveType.intType().type), // Define type as int
                varName, // Variable name
                IntegerLiteralExpr("0") // Initialize to 0
            )
        )
        val update = UnaryExpr(
            NameExpr(varName), // The variable to increment
            UnaryExpr.Operator.POSTFIX_INCREMENT // The '++' operator
        )
        val condition = BinaryExpr(
            NameExpr(varName), // The left operand (variable i)
            IntegerLiteralExpr("0"), // The right operand (literal 0)
            BinaryExpr.Operator.GREATER_EQUALS // The '>=' operator
        )

        val breakCondition = IfStmt()
            .setCondition(negateExpression(whileLoop.condition))
            .setThenStmt(BreakStmt())


        val forloop = ForStmt()
            .setInitialization(NodeList(init))
            .setCompare(condition)
            .setUpdate(NodeList(update))
            .setBody(whileLoop.body)

        forloop.asForStmt().body.asBlockStmt().addStatement(0,breakCondition)


        whileLoop.replace(forloop)


        return Question(
            source,
            TextWithMultipleCodeStatements(
                language["ReplaceWhileWithFor"].format(method.nameAsString),
                listOf(method.toString(), methodReplaced.toString())
            ),
            true.trueOrFalse(language),
            language = language
        )
    }
}

fun main() {
    val source = """
        class abc{
            
            public void test(){
                int a = 3;
                int i = 0;
                int var = 3;
                String var1 = "hello";
                
                    while(i < 3) {
                        System.out.println(i);
                        System.out.println(i+2);
                        i++;
                    }
                a = 6;
                a = 4;
            }
        
        }
    """.trimIndent()
    val qlc = ReplaceWhileWithFor()
    val data = qlc.generate(source, Localisation.getLanguage("pt"))
    println(data)
}
