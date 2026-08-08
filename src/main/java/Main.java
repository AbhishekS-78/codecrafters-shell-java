import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");

            String input = scanner.nextLine();

            // Parse full input into tokens; handles quotes, spaces, concatenation
            List<String> tokens = parseArguments(input);
            if (tokens.isEmpty()) continue;

            String command = tokens.getFirst();
            List<String> argTokens = tokens.subList(1, tokens.size());

            switch (command) {
                case "exit":
                    System.exit(0);

                case "echo":
                    System.out.println(String.join(" ", argTokens));
                    break;

                case "type":
                    String arg = argTokens.isEmpty() ? "" : argTokens.get(0);
                    String[] shellBuiltins = {"exit", "echo", "type", "pwd", "cd"};
                    boolean isShellBuiltin = false;
                    for (String builtin : shellBuiltins) {
                        if (arg.equals(builtin)) { isShellBuiltin = true; break; }
                    }
                    if (isShellBuiltin)
                        System.out.println(arg + " is a shell builtin");
                    else
                        System.out.println(typePath(arg));
                    break;

                case "pwd":
                    System.out.println(System.getProperty("user.dir"));
                    break;

                case "cd":
                    String path = argTokens.isEmpty() ? "~" : argTokens.get(0);
                    File dir = path.startsWith("/") ? new File(path)
                            : path.startsWith("~") ? new File(System.getenv("HOME"))
                              : new File(System.getProperty("user.dir"), path);
                    if (dir.exists() && dir.isDirectory())
                        System.setProperty("user.dir", dir.getCanonicalPath());
                    else
                        System.out.println("cd: " + path + ": No such file or directory");
                    break;

                default:
                    if (getExecutablePath(command) != null) {
                        // Build command array from parsed tokens — preserves quoted args
                        String[] fullCommand = tokens.toArray(new String[0]);
                        runProcess(fullCommand);
                    } else
                        System.out.println(input + ": command not found");
            }
        }
    }

    public static String getExecutablePath(String command) {
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
        String path = getExecutablePath(command);
        if (path != null) return command + " is " + path;
        return command + ": not found";
    }

    public static void runProcess(String[] fullCommand) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(fullCommand);
        pb.inheritIO();
        pb.start().waitFor();
    }

    /**
     * Parses a raw input string into a list of tokens, handling single-quoted or double-quoted strings.
     *
     * <p>Parsing rules:
     * <ul>
     *   <li>Characters inside single/ double quotes are treated literally with no special meaning.</li>
     *   <li>Whitespace inside single/ double quotes is preserved and not used as a delimiter.</li>
     *   <li>Whitespace outside quotes delimits tokens.</li>
     *   <li>Adjacent quoted/unquoted segments with no space between them are concatenated
     *       into a single token. e.g. {@code 'hello''world'} to {@code helloworld}</li>
     * </ul>
     *
     * @param input the full input line including the command
     * @return a list of parsed tokens in order
     */
    public static List<String> parseArguments(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        int i = 0;
        while (i < input.length()) {
            char openingQuote = input.charAt(i);

            if (openingQuote == '\'' || openingQuote == '\"') {
                i++;    // skip opening ' or "
                while (i < input.length() && input.charAt(i) != openingQuote) {
                    current.append(input.charAt(i));
                    i++;
                }
                i++;    // skip closing '
            } else if (openingQuote == ' ') {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                i++;
            } else {
                current.append(openingQuote);
                i++;
            }
        }

        if (!current.isEmpty())
            tokens.add(current.toString());

        return tokens;
    }
}