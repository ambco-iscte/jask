package pt.iscte.pesca.templates

import com.github.javaparser.ast.body.MethodDeclaration
import com.github.javaparser.ast.stmt.*
import pt.iscte.pesca.Language
import pt.iscte.pesca.Localisation
import pt.iscte.pesca.extensions.*

public class ReplaceForWithWhile  : StaticQuestionTemplate<MethodDeclaration>() {

override fun isApplicable(element: MethodDeclaration): Boolean {
    val list = extractForLoops(element)
    return list.isNotEmpty()
}

override fun build(sources: List<SourceCode>, language: Language): Question {
    val (source, method) = sources.getRandom<MethodDeclaration>()

    val methodReplaced = method.clone()

    var forLoops = extractForLoops(methodReplaced)

    println(forLoops)

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
                int a = 3;
                for (int i = 0; i < 10; i++){
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
