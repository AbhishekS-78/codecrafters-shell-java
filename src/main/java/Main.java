import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.print("$ ");

            String input = scanner.nextLine();
            List<String> tokens = parseArguments(input);
            if (tokens.isEmpty()) continue;

            // Detect > or 1> or 2> redirection in token list
            String outputFile = null;
            String errorFile = null;
            boolean appendOutput = false;
            List<String> cmdTokens = new ArrayList<>();
            for (int i = 0; i < tokens.size(); i++) {
                String t = tokens.get(i);
                if ((t.equals(">>") || t.equals("1>>")) && i + 1 < tokens.size()) {
                    outputFile = tokens.get(++i);
                    appendOutput = true;
                } else if ((t.equals(">") || t.equals("1>")) && i + 1 < tokens.size()) {
                    outputFile = tokens.get(++i);
                    appendOutput = false;
                } else if (t.equals("2>") && i + 1 < tokens.size()) {
                    errorFile = tokens.get(++i);
                } else {
                    cmdTokens.add(t);
                }
            }

            String command = cmdTokens.get(0);
            List<String> argTokens = cmdTokens.subList(1, cmdTokens.size());

            // If redirecting, swap System.out to the target file for builtins
            PrintStream originalOut = System.out;
            PrintStream originalErr = System.err;
            if (outputFile != null) System.setOut(new PrintStream(new java.io.FileOutputStream(outputFile, appendOutput)));
            if (errorFile != null) System.setErr(new PrintStream(new java.io.FileOutputStream(errorFile, false)));

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
                        // cd errors go to stderr — not affected by stdout redirect
                        System.err.println("cd: " + path + ": No such file or directory");
                    break;

                default:
                    if (getExecutablePath(command) != null) {
                        ProcessBuilder pb = new ProcessBuilder(cmdTokens);
                        // pb.redirectOutput lives here, inside default, where pb exists
                        if (outputFile != null) pb.redirectOutput(appendOutput
                                ? ProcessBuilder.Redirect.appendTo(new File(outputFile))
                                : ProcessBuilder.Redirect.to(new File(outputFile)));
                        else pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
                        if (errorFile != null) pb.redirectError(new File(errorFile));
                        else pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                        pb.start().waitFor();
                    } else {
                        System.setOut(originalOut);
                        System.out.println(input + ": command not found");
                    }
            }

            // Restore System.out after builtin runs
            System.setOut(originalOut);
            System.setErr(originalErr);
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
            char c = input.charAt(i);

            if (c == '\'' || c == '"') {
                char openingQuote = c;
                i++;
                while (i < input.length() && input.charAt(i) != openingQuote) {
                    if (openingQuote == '"') {
                        if (input.charAt(i) == '\\' && i + 1 < input.length()) {
                            char next = input.charAt(i + 1);
                            if (next == '"' || next == '\\') {
                                current.append(next);
                                i += 2;
                            } else {
                                current.append(input.charAt(i));
                                i++;
                            }
                        } else {
                            current.append(input.charAt(i));
                            i++;
                        }
                    } else {
                        // single quotes: everything literal
                        current.append(input.charAt(i));
                        i++;
                    }
                }
                i++; // skip closing quote
            } else if (c == '\\') {
                current.append(input.charAt(i + 1));
                i += 2;
            } else if (c == ' ') {
                if (!current.isEmpty()) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
                i++;
            } else {
                current.append(c);
                i++;
            }
        }

        if (!current.isEmpty())
            tokens.add(current.toString());

        return tokens;
    }
}
