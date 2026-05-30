import java.io.File;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

//        REPL: read-eval-print-loop
        while (true) {
            System.out.print("$ ");

            String command = scanner.nextLine(),
                    firstWord = !command.contains(" ") ? command : command.substring(0, command.indexOf(" ")),
                    remaining = !command.contains(" ") ? "" : command.substring(command.indexOf(" ") + 1);

//            exit command
            switch (firstWord) {
                case "exit":
                    System.exit(0);
                case "echo":
//                echo command
                    System.out.println(remaining);
                    break;
                case "type":
//                type command
                    boolean isShellBuiltin = remaining.equals("exit") || remaining.equals("echo") || remaining.equals("type");
                    if (isShellBuiltin)
                        System.out.println(remaining + " is a shell builtin");
                    else
                        System.out.println(typePath(remaining));
                    break;
                default:
                    System.out.println(command + ": command not found");
            }
        }
    }

//    type command extension for executable files using PATH
    public static String typePath(String command) {
//        Get PATH env
        String path = System.getenv("PATH");
        String[] pathDirs = path.split(File.pathSeparator);

        // Windows executable extensions to try
        String[] extensions = System.getProperty("os.name").toLowerCase().contains("win")
                ? new String[]{"", ".exe", ".cmd", ".bat"}
                : new String[]{""};

        for (String pathDir : pathDirs) {
            for (String ext : extensions) {
                File file = new File(pathDir, command + ext);
                if (file.exists() && file.canExecute())
                    return command + " is " + file.getAbsolutePath();
            }
        }

        return command + ": not found";
    }
}
