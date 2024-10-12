public class HelloWorld {

    public static int factorial(int n) {
        if (n == 0) return 1;
        else return n * factorial(n - 1);
    }

    public static int square(int n) {
        return n * n;
    }

    public static int sum(int n, int m) {
        return n + m;
    }

    public static void hello() {
        System.out.println("Hello World!");
    }
}