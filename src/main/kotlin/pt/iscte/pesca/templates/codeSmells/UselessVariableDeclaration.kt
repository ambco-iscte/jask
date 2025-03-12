package pt.iscte.pesca.templates

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.*
import pt.iscte.pesca.Language
import pt.iscte.pesca.Localisation
import pt.iscte.pesca.extensions.*

class UselessVariableDeclaration : StaticQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean {
        val list = findUselessVariableDeclarations(element)
        return list.isNotEmpty()
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()

        var StmtPairs = findUselessVariableDeclarations(methodReplaced)

        while (StmtPairs.size > 0) {
            for (stmtPair in StmtPairs) {
                val newStmt = mergeVariableDeclaration(stmtPair)
                if (stmtPair.first.replace(newStmt))
                    stmtPair.second.remove()
            }
            StmtPairs = findUselessVariableDeclarations(methodReplaced)
        }

        return Question(
            source,
            TextWithMultipleCodeStatements(
                language["UselessVariableDeclaration"].format(method.nameAsString),
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
                int y;
                int uses_y = y;
                
                if(true){
                    int a;
                    a = 2;
                }
                
                y = 0;
                int b;
                b = y + 10;
                b = 22;
                y = 2;
            }
        
        }
    """.trimIndent()
    val qlc = UselessVariableDeclaration()
    val data = qlc.generate(source, Localisation.getLanguage("pt"))
    println(data)
}