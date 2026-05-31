import java.io.File;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");

            // Split input into command and arguments
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
                    // Check if argument is a builtin, otherwise search PATH
                    String[] shellBuiltins = {"exit", "echo", "type", "pwd", "cd", "mkdir"};
                    boolean isShellBuiltin = false;

                    for (String shellBuiltin : shellBuiltins) {
                        if (arguments.equals(shellBuiltin)) {
                            isShellBuiltin = true;
                            break;
                        }
                    }
                    if (isShellBuiltin)
                        System.out.println(arguments + " is a shell builtin");
                    else
                        System.out.println(typePath(arguments));
                    break;

                case "pwd":
                    System.out.println(System.getProperty("user.dir"));
                    break;

                case "cd":
                    if (arguments.startsWith("/")) {
                        File file = new File(arguments);
                        if (file.exists() && file.isDirectory())
                            System.setProperty("user.dir", file.getAbsolutePath());
                        else
                            System.out.println("cd: " + arguments + ": No such file or directory");
                    } else
                        System.out.println("cd: " + arguments + ": No such file or directory");
                    break;

                default:
                    // For non-builtins: verify executable exists, then run it
                    if (getExecutable(command) != null) {
                        String[] commandArgs = arguments.split(" "),
                                fullCommand = new String[1 + commandArgs.length];
                        fullCommand[0] = command;
                        System.arraycopy(commandArgs, 0, fullCommand, 1, commandArgs.length);

                        runProcess(fullCommand);
                    } else
                        System.out.println(input + ": command not found");
            }
        }
    }

    // Walk each directory in PATH, return absolute path if executable found
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

    // Return "cmd is /path/to/cmd" if found in PATH, else "cmd: not found"
    public static String typePath(String command) {
        String path = getExecutable(command);
        if (path != null) return command + " is " + path;

        return command + ": not found";
    }

    public static void runProcess(String[] fullCommand) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(fullCommand);
        pb.inheritIO();         // Wire child process stdio to shell's stdio
        pb.start().waitFor();   // Wait for process to finish before next prompt
    }
}
