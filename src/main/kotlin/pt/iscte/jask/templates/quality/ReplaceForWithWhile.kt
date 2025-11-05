package pt.iscte.jask.templates.quality
import pt.iscte.jask.templates.*

import com.github.javaparser.ast.NodeList
import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.nodeTypes.NodeWithBody
import com.github.javaparser.ast.stmt.*
import pt.iscte.jask.Language
import pt.iscte.jask.Localisation
import pt.iscte.jask.extensions.*

public class ReplaceForWithWhile  : StructuralQuestionTemplate<MethodDeclaration>() {

    override fun isApplicable(element: MethodDeclaration): Boolean {
        val list = element.findAll(ForStmt::class.java)
        return list.isNotEmpty()
    }

    override fun build(sources: List<SourceCode>, language: Language): Question {
        val (source, method) = sources.getRandom<MethodDeclaration>()

        val methodReplaced = method.clone()

        var forLoop = methodReplaced.findAll(ForStmt::class.java).random()


        val whileStmt = WhileStmt()
            .setCondition(forLoop.compare.get())
            .setBody(forLoop.body)
        forLoop.update.forEach { up ->
            whileStmt.body.asBlockStmt().addStatement(up)
        }

        val parent = forLoop.parentNode.get()

        if (parent is BlockStmt) {
            val parentBlock = parent as BlockStmt
            val indexOfFor = parentBlock.childNodes.indexOf(forLoop)
            forLoop.replace(whileStmt)
            forLoop.initialization.reversed().forEach { parentBlock.addStatement(indexOfFor, it) }
        } else if (parent is IfStmt) {
            if (parent.thenStmt.equals(forLoop)){
                parent.setThenStmt(BlockStmt(NodeList(forLoop.initialization.reversed().map { ExpressionStmt(it) } + listOf(whileStmt))))
            }else{
                parent.setElseStmt(BlockStmt(NodeList(forLoop.initialization.reversed().map { ExpressionStmt(it) } + listOf(whileStmt))))
            }
        } else if(parent is NodeWithBody<*>){
            parent.setBody(BlockStmt(NodeList(forLoop.initialization.reversed().map { ExpressionStmt(it) } + listOf(whileStmt))))
        } else {
            val newParent = BlockStmt(NodeList(forLoop.initialization.reversed().map { ExpressionStmt(it) } + listOf(whileStmt)))
            parent.replace(newParent)
        }





        return Question(
                source,
                TextWithMultipleCodeStatements(
                        language["ReplaceForWithWhile"].format(method.nameAsString),
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
                
                    for ( i = 3; i < 10; i++) {
                        System.out.println(i);
                        System.out.println(i+2);
                    }
                a = 6;
                a = 4;
            }
        
        }
    """.trimIndent()
    val qlc = ReplaceForWithWhile()
    val data = qlc.generate(source, Localisation.getLanguage("pt"))
    println(data)
}
