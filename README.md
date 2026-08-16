# codecrafters-shell-java

A POSIX-style shell built from scratch in Java. Follows the [codecrafters.io shell challenge](https://codecrafters.io/challenges/shell).

---

## Features

- REPL with prompt
- Builtins: `exit`, `echo`, `type`, `pwd`, `cd`
- External program execution via PATH resolution
- Quote parsing: single quotes, double quotes, backslash escaping
- I/O redirection: `>`, `>>`, `2>`, `2>>` (and `1>`, `1>>` variants)

---

## Implementation Notes

### REPL

Input is parsed into tokens via `parseArguments`, which handles quotes and escapes. The first token is the command, the rest are arguments.

### Builtins

Handled directly in Java, no subprocess.

### `type`

Walks each directory in `$PATH` looking for a matching executable. Reports builtin, external binary, or not found.

### External program execution

`ProcessBuilder` is used over `Runtime.exec()` for stdio control. The command name as typed is passed as `argv[0]` -- the OS resolves PATH itself.

```java
fullCommand[0] = command;   // "cat" -- argv[0], not "/usr/bin/cat"
ProcessBuilder pb = new ProcessBuilder(fullCommand);
pb.inheritIO();
pb.start().waitFor();
```

### `pwd`

```java
System.out.println(System.getProperty("user.dir"));
```

### `cd`

Handles absolute paths, relative paths (`./`, `../`, subdirectories), and `~` (expands to `$HOME`). Uses `getCanonicalPath()` to resolve `../` and `./` correctly.

```java
File dir = path.startsWith("/") ? new File(path)
         : path.startsWith("~") ? new File(System.getenv("HOME"))
         : new File(System.getProperty("user.dir"), path);
```

### Quote parsing

`parseArguments` walks the input character by character:

- Single quotes: everything inside is literal, no exceptions
- Double quotes: everything literal except `\"` and `\\`
- Backslash outside quotes: escapes the next character, backslash is removed
- Adjacent quoted/unquoted segments with no space are concatenated into one token

### I/O redirection

Redirection tokens (`>`, `>>`, `2>`, `2>>`) are detected before dispatch and stripped from the command token list.

For builtins, `System.out` and `System.err` are swapped via `System.setOut` and `System.setErr` before the command runs, then restored after.

For externals, `ProcessBuilder.redirectOutput` and `ProcessBuilder.redirectError` handle it directly. Append mode uses `ProcessBuilder.Redirect.appendTo`.

---

## Stack

- Java 25 (Amazon Corretto)
- Maven
- Linux (Ubuntu)

## Run locally

```bash
git clone https://github.com/AbhishekS-78/codecrafters-shell-java
cd codecrafters-shell-java
mvn compile
./your_program.sh
```
