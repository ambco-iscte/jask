package pt.iscte.pesca.compiler

import com.github.javaparser.ParserConfiguration
import com.github.javaparser.StaticJavaParser
import com.github.javaparser.symbolsolver.JavaSymbolSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver
import pt.iscte.pesca.Localisation
import pt.iscte.pesca.questions.compiler.CallMethodWithWrongParameterNumber
import pt.iscte.pesca.questions.compiler.CallMethodWithWrongParameterTypes
import pt.iscte.pesca.questions.compiler.MethodWithWrongReturnStmt

fun main() {
    StaticJavaParser.getParserConfiguration().languageLevel = ParserConfiguration.LanguageLevel.JAVA_20
    StaticJavaParser.getParserConfiguration().setSymbolResolver(
        JavaSymbolSolver(CombinedTypeSolver().apply { add(ReflectionTypeSolver()) })
    )

    val src = """
        class StaticMethods {
            public static int square(int n) {
                return n * n;
            }
            
            public int floor(double x) {
                int k = "hello";
                return (int) x;
            }
            
            public static String hello() {
                return 2;
            }
        }
        
        class VeryUsefulMath {
            private int k = 2;
            private int m = 2 * l;
            private int a = 2 * a;
            
            private notfoundclass x = 3;
            private doesnotexist y = 4;
            
            private int zz = "Hello World!";
            private int ww = 3.14;
            
            public static int bar(int n) {
                unsigned_int m = 400000;
                return n;
            }
        
            public static int doubleOf(int n) {
                foo("Hello world!", bar(n));
                System.out.println(r);
                for (int j = 0; j < 10; j++) {
                    bar(j);
                }
                return i * 2 * n;
            }
            
            public static int squareRoot(int n) {
                int x = bar(1, 2);
                int y = bar("hello");
                return Math.sqrt(n);
            }
        }
    """.trimIndent()

    val qlc = CallMethodWithWrongParameterTypes()
    val data = qlc.generate(src, Localisation.getLanguage("pt"))

    println(data)
}