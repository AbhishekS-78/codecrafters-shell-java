import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {
//        REPL: read-eval-print-loop
        while (true) {
            System.out.print("$ ");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();

//            exit command
            if (input.equalsIgnoreCase("exit")) {
                break;
            } else if (input.startsWith("echo")) {
//                echo command
                System.out.println(input.substring(5));
            } else if (input.startsWith("type")) {
//                type command
                if (input.substring(5).equalsIgnoreCase("echo")) {
                    System.out.println("echo is a shell builtin");
                } else if (input.substring(5).equalsIgnoreCase("exit")) {
                    System.out.println("exit is a shell builtin");
                } else if (input.substring(5).equalsIgnoreCase("type")) {
                    System.out.println("type is a shell builtin");
                } else {
                    System.out.println(input.substring(5) + ": not found");
                }
            } else {
                System.out.println(input + ": command not found");
            }
        }
    }
}
