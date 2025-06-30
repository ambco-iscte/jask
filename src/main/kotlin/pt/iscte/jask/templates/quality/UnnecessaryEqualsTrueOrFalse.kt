package pt.iscte.jask.templates.quality
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.IfStmt
import pt.iscte.jask.Language
import pt.iscte.jask.Localisation
import pt.iscte.jask.extensions.*

class UnnecessaryEqualsTrueOrFalse : StaticQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean {
        val ifStmt = element.findAll(IfStmt::class.java).firstOrNull {
            val condition = it.condition
            removeEqualsTrueOrFalse(condition) != condition
        }
        return ifStmt != null
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()
        val methodReplacedWA = method.clone()
        val methodReplacedWA2 = method.clone()

        var ifStmt = methodReplaced.findAll(IfStmt::class.java).first {
            val condition = it.condition
            removeEqualsTrueOrFalse(condition) != condition
        }
        var ifStmtWA = methodReplacedWA.findAll(IfStmt::class.java).first {
            val condition = it.condition
            removeEqualsTrueOrFalse(condition) != condition
        }
        var ifStmtWA2 = methodReplacedWA2.findAll(IfStmt::class.java).first {
            val condition = it.condition
            removeEqualsTrueOrFalse(condition) != condition
        }


        do {
            ifStmt?.setCondition(removeEqualsTrueOrFalse(ifStmt.condition))
            ifStmt = methodReplaced.findAll(IfStmt::class.java).firstOrNull {
                val condition = it.condition
                removeEqualsTrueOrFalse(condition) != condition
            }
        } while (ifStmt != null)
        do {
            ifStmtWA?.setCondition(removeEqualsTrueOrFalse(negateExpression(ifStmtWA.condition)))
            ifStmtWA = methodReplacedWA.findAll(IfStmt::class.java).firstOrNull {
                val condition = it.condition
                removeEqualsTrueOrFalse(condition) != condition
            }
        } while (ifStmtWA != null)
        do {
            replaceIfWithThenBody(ifStmtWA2)
            ifStmtWA2 = methodReplacedWA2.findAll(IfStmt::class.java).firstOrNull {
                val condition = it.condition
                removeEqualsTrueOrFalse(condition) != condition
            }
        } while (ifStmtWA2 != null)


        return Question(
            source,
            TextWithMultipleCodeStatements(
                language["UnnecessaryEqualsTrueOrFalse"].format(method.nameAsString),
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
            
            public int test(){
                int y = 0;
                if(a == true )
                    return 1;
                y = 2;
                return y;
            }
        
        }
    """.trimIndent()

    val qlc = UnnecessaryEqualsTrueOrFalse()
    val data = qlc.generate(source, Localisation.getLanguage("pt"))
    println(data)
}
