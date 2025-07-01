package pt.iscte.jask.templates.quality
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.*
import pt.iscte.jask.Language
import pt.iscte.jask.Localisation
import pt.iscte.jask.extensions.*

class LonelyVariable : StructuralQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean {
        val list = findLonelyVariables(element)
        return list.isNotEmpty()
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()
        val lonelyVariables = findLonelyVariables(methodReplaced)
        lonelyVariables.forEach {
            val stmts= findStatementsUsingVariables(methodReplaced, listOf(it))
            stmts.forEach { it.remove() }
        }

        val methodReplacedWA = method.clone()
        val lonelyVariablesWA = findLonelyVariables(methodReplacedWA)
        val lonelyVarWA = lonelyVariablesWA.random()

        lonelyVariablesWA.filter { !it.equals(lonelyVarWA) }.forEach {
            val stmts= findStatementsUsingVariables(methodReplacedWA, listOf(it))
            stmts.forEach { it.remove() }
        }


        val lonelyVarWAType = methodReplacedWA.getLocalVariables().first{
            it.name.toString() == lonelyVarWA
        }.type


        methodReplacedWA.setType(lonelyVarWAType)
        val returnStatements = methodReplacedWA.findAll(com.github.javaparser.ast.stmt.ReturnStmt::class.java)
        val newReturn = ReturnStmt(lonelyVarWA.toString())
        if (returnStatements.isEmpty()) {
            methodReplacedWA.body.get().addStatement(newReturn)
        }else{
            returnStatements.forEach { returnStatement -> returnStatement.replace(newReturn) }
        }


        return Question(
            source,
            TextWithMultipleCodeStatements(
                language["LonelyVariable"].format(method.nameAsString),
                listOf(method.toString())
            ),
            mapOf(
                SimpleTextOption(methodReplaced.toString()) to true,
                SimpleTextOption(methodReplacedWA.toString() ) to false
            ),
            language = language
        )
    }
}

fun main() {
    val source = """
        class abc{
            
            public void test(){
                boolean used;
                if(used){
                    int lonely = 2;
                    lonely = 4;
                    used = !used;
                }
                int lonely2;
            }
        
        }
    """.trimIndent()
    val qlc = LonelyVariable()
    val data = qlc.generate(source, Localisation.getLanguage("pt"))
    println(data)
}