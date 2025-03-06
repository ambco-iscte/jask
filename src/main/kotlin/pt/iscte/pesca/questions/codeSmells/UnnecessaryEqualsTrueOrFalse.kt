package pt.iscte.pesca.questions

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.IfStmt
import pt.iscte.pesca.Language
import pt.iscte.pesca.Localisation
import pt.iscte.pesca.extensions.*

class UnnecessaryEqualsTrueOrFalse : StaticQuestion<MethodDeclaration>() {

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

        do {
            ifStmt?.setCondition(removeEqualsTrueOrFalse(ifStmt.condition))
            ifStmt = methodReplaced.findAll(IfStmt::class.java).firstOrNull {
                val condition = it.condition
                if (removeEqualsTrueOrFalse(condition) != condition) {
                    true
                } else {
                    false
                }
            }
        } while (ifStmt != null)


        return QuestionData(
            source,
            TextWithMultipleCodeStatements(
                language["UnnecessaryEqualsTrueOrFalse"].format(method.nameAsString),listOf(method.toString(),methodReplaced.toString())),
            true.trueOrFalse(language),
            language = language,

            )
    }
}



fun main() {
    val source = """
        class abc{
            
            public void test(){
                int y = 0;
                if(a == true || a != true){
                    //todo
                }
                if(false != a &&  a == false){
                    //todo
                }
            }
        
        }
    """.trimIndent()

    val qlc = UnnecessaryEqualsTrueOrFalse()
    val data = qlc.generate(source,Localisation.getLanguage("pt"))
    println(data)
}
