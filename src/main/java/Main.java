import java.io.File;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");

            String input = scanner.nextLine(),
                    command = !input.contains(" ") ? input : input.substring(0, input.indexOf(" ")),
                    arguments = !input.contains(" ") ? "" : input.substring(input.indexOf(" ") + 1);

            switch (command) {
                case "exit":
                    System.exit(0);
                case "echo":
                    System.out.println(arguments);
                    break;
                case "type":
                    boolean isShellBuiltin = arguments.equals("exit") || arguments.equals("echo") || arguments.equals("type");
                    if (isShellBuiltin)
                        System.out.println(arguments + " is a shell builtin");
                    else
                        System.out.println(typePath(arguments));
                    break;
                default:
                    command = getExecutable(command);       // Check if the command is an executable
                    if (command != null) {
//                        Restructure the command to get the arguments right
                        String[] commandArgs = arguments.split(" "),
                                fullCommand = new String[1 + commandArgs.length];
                        fullCommand[0] = command;
                        System.arraycopy(commandArgs, 0, fullCommand, 1, commandArgs.length);

                        ProcessBuilder pb = new ProcessBuilder(fullCommand);
                        pb.command().set(0, command);
                        pb.inheritIO();
                        pb.start().waitFor();
                    } else
                        System.out.println(input + ": command not found");
            }
        }
    }

    public static String getExecutable(String command) {
        String path = System.getenv("PATH");
        String[] pathDirs = path.split(File.pathSeparator);

        for (String pathDir : pathDirs) {
            File file = new File(pathDir, command);
            if (file.exists() && file.canExecute())
                return file.getAbsolutePath();
        }

        return null;
    }

    public static String typePath(String command) {
        String path = getExecutable(command);
        if (path != null) return command + " is " + path;

        return command + ": not found";
    }
}
