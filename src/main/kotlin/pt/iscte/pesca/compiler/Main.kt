package pt.iscte.pesca.compiler

import pt.iscte.pesca.Localisation
import pt.iscte.pesca.extensions.configureStaticJavaParser
import pt.iscte.pesca.questions.compiler.AssignVarWithMethodWrongType
import pt.iscte.pesca.questions.compiler.CallMethodWithWrongParameterNumber

fun main() {
    configureStaticJavaParser()

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
            
            private String abc = bar(4);
            
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

    val qlc = CallMethodWithWrongParameterNumber()
    val data = qlc.generate(src, Localisation.getLanguage("pt"))

    println(data)
}