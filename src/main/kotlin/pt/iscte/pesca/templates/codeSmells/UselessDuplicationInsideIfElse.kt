package pt.iscte.pesca.templates

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.nodeTypes.NodeWithBody
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.IfStmt
import pt.iscte.pesca.Language
import pt.iscte.pesca.Localisation
import pt.iscte.pesca.extensions.*

class UselessDuplicationInsideIfElse : StaticQuestionTemplate<MethodDeclaration>() {


    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.hasDuplicatedInsideIfElse() != null

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()
        val methodReplacedWA = method.clone()
        val methodReplacedWA2 = method.clone()



        refactorIfByExtractingCommonParts(methodReplaced.hasDuplicatedInsideIfElse()!!)
        val ifStmtWA = refactorIfByExtractingCommonParts(methodReplacedWA.hasDuplicatedInsideIfElse()!!)
        ifStmtWA.removeElseStmt()

        val ifStmtWA2 = refactorIfByExtractingCommonParts(methodReplacedWA2.hasDuplicatedInsideIfElse()!!)
        ifStmtWA2.remove()

        return Question(
            source,
            TextWithMultipleCodeStatements(
                language["UselessDuplicationInsideIfElse"].format(method.nameAsString),
                listOf(method.toString())
            ),
            mapOf(
                SimpleTextOption(methodReplaced.toString()) to true,
                SimpleTextOption(methodReplacedWA.toString()) to false,
                SimpleTextOption(methodReplacedWA2.toString()) to false
            ),
            language = language

        )
    }
}

fun main() {
    val source = """
        class abc{
            
            public void test(){
                if(a == true){
                    print("first");
                    print("oops");
                    print("last");
                }
                else {
                    print("first");
                    print("not the same");
                    print("last");
                }
            }
        
        }
    """.trimIndent()

    val qlc = UselessDuplicationInsideIfElse()
    val data = qlc.generate(source, Localisation.getLanguage("pt"))
    println(data)
}
