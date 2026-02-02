package pt.iscte.jask.templates.quality
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.NullLiteralExpr
import pt.iscte.jask.Language
import pt.iscte.jask.Localisation
import pt.iscte.jask.extensions.*
import pt.iscte.jask.common.Question
import pt.iscte.jask.common.QuestionOption
import pt.iscte.jask.common.SimpleTextOption
import pt.iscte.jask.common.SourceCode
import pt.iscte.jask.common.TextWithMultipleCodeStatements

class UselessSelfAssign : StructuralQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean {
        val list = getSelfAssignments(element)
        print(list)
        return list.isNotEmpty()
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced0 = method.clone()
        val methodReplaced1 = method.clone()

        getSelfAssignments(methodReplaced0).sample(1)!!.random()
            .remove()
        getSelfAssignments(methodReplaced1).sample(1)!!.random()
            .asExpressionStmt().getExpression().asAssignExpr().setValue(NullLiteralExpr())





        var options: MutableMap<QuestionOption, Boolean> = mutableMapOf(
            SimpleTextOption(methodReplaced0.toString()) to true,
            SimpleTextOption(methodReplaced1.toString()) to false,
            SimpleTextOption(language["NoneOfTheAbove"]) to false
        )

        return Question(
            source,
            TextWithMultipleCodeStatements(
                language["UselessSelfAssign"].format(method.nameAsString),
                listOf(method.toString())
            ),
            options,
            language = language
        )
    }
}

fun main() {
    val source = """
        class abc{
            
            public void test(){
                int y;
                if(true){
                    int a;
                    a = a;
                }
                int b;
                b = 22;
                y = y;
            }
        
        }
    """.trimIndent()
    val qlc = UselessSelfAssign()
    val data = qlc.generate(source, Localisation.getLanguage("pt"))
    println(data)
}