package pt.iscte.pesca.questions.codeSmells

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.IfStmt
import pt.iscte.pesca.Language
import pt.iscte.pesca.Localisation
import pt.iscte.pesca.extensions.removeEqualsTrueOrFalse
import pt.iscte.pesca.extensions.trueOrFalse
import pt.iscte.pesca.questions.*

class UselessVariableDeclaration : StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean {
        val ifStmt = element.findAll(IfStmt::class.java).firstOrNull {
            val condition = it.condition
            if (removeEqualsTrueOrFalse(condition) != condition) {
                true
            } else {
                false
            }
        }
        return ifStmt != null
    }

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()

        var ifStmt = methodReplaced.findAll(IfStmt::class.java).firstOrNull {
            val condition = it.condition
            if (removeEqualsTrueOrFalse(condition) != condition) {
                true
            } else {
                false
            }
        }

        ifStmt?.setCondition(removeEqualsTrueOrFalse(ifStmt.condition))

        return QuestionData(
            source,
            TextWithMultipleCodeStatements(
                language["UselessVariableDeclaration"].format(method.nameAsString),listOf(method.toString(),methodReplaced.toString())),
            true.trueOrFalse(language),
            language = language,

            )
    }
}



fun main() {
    val source = """
        class abc{
            
            public void test(){
                int y;
                y = 0;
                // int y = 0;
                
                int a;
                if(true)
                    a = 2;
                // cant change
                    
                int f;
                for(int i; i < 10; i++)
                    f += f;
                // cant change
                
                int b;
                b = y + 10;
                //int b =  y + 10;
            }
        
        }
    """.trimIndent()

    val qlc = UselessVariableDeclaration()
    val data = qlc.generate(source, Localisation.getLanguage("pt"))
    println(data)
}