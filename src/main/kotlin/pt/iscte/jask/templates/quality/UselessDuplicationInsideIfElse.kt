package pt.iscte.jask.templates.quality
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import pt.iscte.jask.Language
import pt.iscte.jask.Localisation
import pt.iscte.jask.extensions.*

class UselessDuplicationInsideIfElse : StructuralQuestionTemplate<MethodDeclaration>() {


    override fun isApplicable(element: MethodDeclaration): Boolean =
        element.hasDuplicatedInsideIfElse() != null

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()
        val methodReplacedWA = method.clone()
        val methodReplacedWA2 = method.clone()





        val ifStmtWA2 = refactorIfByExtractingCommonParts(methodReplacedWA2.hasDuplicatedInsideIfElse()!!)
        ifStmtWA2.remove()

        var options = mapOf<Option,Boolean>()

        refactorIfByExtractingCommonParts(methodReplaced.hasDuplicatedInsideIfElse()!!)
        val ifStmtWA = refactorIfByExtractingCommonParts(methodReplacedWA.hasDuplicatedInsideIfElse()!!)
        if (ifStmtWA.elseStmt.get().asBlockStmt().isEmpty) {
            options = mapOf<Option,Boolean>(
                SimpleTextOption(methodReplaced.toString()) to true,
                SimpleTextOption(methodReplacedWA2.toString()) to false,
                SimpleTextOption(language["NoneOfTheAbove"]) to false
            )


        }else{
            ifStmtWA.removeElseStmt()
            options = mapOf<Option,Boolean>(
                SimpleTextOption(methodReplaced.toString()) to true,
                SimpleTextOption(methodReplacedWA2.toString()) to false,
                SimpleTextOption(methodReplacedWA) to false
            )
        }


        return Question(
            source,
            TextWithMultipleCodeStatements(
                language["UselessDuplicationInsideIfElse"].format(method.nameAsString),
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
                if(a == true){
                    print("first");
                    print("oops");
                    print("last");
                }
                else {
                    print("first");
                    print("not equal");
                    print("last");
                }
            }
        
        }
    """.trimIndent()

    val qlc = UselessDuplicationInsideIfElse()
    val data = qlc.generate(source, Localisation.getLanguage("pt"))
    println(data)
}
