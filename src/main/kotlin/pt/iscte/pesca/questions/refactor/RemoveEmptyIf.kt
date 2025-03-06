package pt.iscte.pesca.questions

import pt.iscte.pesca.questions.QuestionData
import pt.iscte.pesca.questions.SourceCode
import pt.iscte.pesca.questions.StaticQuestion
import pt.iscte.pesca.questions.TextWithMultipleCodeStatements

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.nodeTypes.NodeWithBody
import com.github.javaparser.ast.stmt.BlockStmt
import com.github.javaparser.ast.stmt.IfStmt
import pt.iscte.pesca.Language
import pt.iscte.pesca.Localisation
import pt.iscte.pesca.extensions.*

class RemoveEmptyIf : StaticQuestion<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean {
        val ifStmt = element.findAll(IfStmt::class.java).firstOrNull {
            if (it.thenStmt.isBlockStmt) {
                it.thenStmt.asBlockStmt().isEmpty
            } else {
                false
            }
        }
        return ifStmt != null
    }

    override fun build(sources: List<SourceCode>, language: Language): QuestionData {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()

        val ifStmt = methodReplaced.findAll(IfStmt::class.java).first {
            if (it.thenStmt.isBlockStmt) {
                it.thenStmt.asBlockStmt().isEmpty
            } else {
                false
            }
        }

        if (ifStmt.elseStmt.isPresent) {
            TODO()
        }else{
            val thenStmt = ifStmt.thenStmt
            val parent = ifStmt.parentNode.get()
            if (parent is BlockStmt){
                val parentStatements = parent.asBlockStmt().statements
                parentStatements.remove(ifStmt)
            }else{
                if (parent is NodeWithBody<*>){
                    (parent as NodeWithBody<*>).setBody(thenStmt)
                }
                if (parent is IfStmt)
                    if((parent).thenStmt.equals(ifStmt)){
                        (parent).thenStmt.replace(thenStmt)
                    }else{
                        (parent).elseStmt.get().replace(thenStmt)
                    }
            }
        }


        return QuestionData(
            source,
            TextWithMultipleCodeStatements(
                language["UselessDuplicationIfElse"].format(method.nameAsString),listOf(method.toString(),methodReplaced.toString())),
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
                if(a == true){
                    
                }
            }
        
        }
    """.trimIndent()

    val qlc = RemoveEmptyIf()
    val data = qlc.generate(source,Localisation.getLanguage("pt"))
    println(data)
}
