import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {
        while (true) {
            System.out.print("$ ");
            Scanner scanner = new Scanner(System.in);
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("exit")) break;

            if (input.startsWith("echo")) {
                String[] argument = input.split(" ", 2);
                System.out.print(argument[1]);
                System.out.println();
            } else {
                System.out.println(input + ": command not found");
            }
        }
    }
}
