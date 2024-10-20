public class HelloWorld {

    public static int factorial(int n) {
        if (n == 0) {
            return 1;
        }
        else {
            int fact = factorial(n - 1);
            return n * fact;
        }
    }

    public static int square(int n) {
        return n * n;
    }

    public static int sum(int n, int m) {
        return n + m;
    }

    public static void hello() {
        String helloWorld = "Hello World!";
        System.out.println(helloWorld);
    }

    public static int howManyPositiveEvensNumbersBeforeN(int n) {
        int count = 0;
        for (int i = 1; i <= n; i++) {
            if (n%2) count++;
        }
        return count;
    }

    public static void printHelloNTimes(int n){
        while (n > 0){
            hello();
            n--;
        }
    }

    public static int whileAndForMethod() {
        int twoLoops = 2;
        while (twoLoops > 0) {
            for (int i = 1; i <= 100; i++) {
                if (i == 50) {
                    twoLoops--;
                }
            }
        }
    }

    public static String printTimesN(String text, int times){
        return text.repeat(times);
    }
}