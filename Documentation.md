# codecrafters-shell-java

Stage-by-stage implementation log. Decisions, bugs, and what each stage required.

---

## Base Stages

### Stage 1-3: Prompt, Invalid Commands, REPL

Standard `while(true)` loop. Input is fed into `parseArguments` which returns a token list. First token is the command, rest are arguments. Unknown commands fall through to `default` and print `<input>: command not found`.

---

### Stage 4-5: `exit` and `echo`

`exit 0` calls `System.exit(0)`. `echo` joins argument tokens with a single space. Both are pure Java, no subprocess.

---

### Stage 6-7: `type` and PATH resolution

`type` checks builtins first, then calls `getExecutablePath` which walks `$PATH` directories looking for a matching executable file.

```java
for (String pathDir : pathDirs) {
    File file = new File(pathDir, command);
    if (file.exists() && file.canExecute())
        return file.getAbsolutePath();
}
```

`getExecutablePath` is reused by both `type` and the execution stage.

---

### Stage 8: Running external programs

The key issue: `argv[0]` must be the command name as typed, not the resolved absolute path. The tester verifies this explicitly.

Initial approach used `Runtime.exec()` with the full resolved path, which broke `argv[0]`. Switched to `ProcessBuilder`.

Passing the command name directly works because the OS resolves PATH itself when launching the process.

```java
fullCommand[0] = command;        // "cat" -- what the process sees as argv[0]
ProcessBuilder pb = new ProcessBuilder(fullCommand);
pb.inheritIO();                  // wire child stdio to shell stdio
pb.start().waitFor();            // block until process exits
```

`pb.inheritIO()` replaced the earlier `getInputStream().transferTo(System.out)` -- cleaner and handles stderr too.

Bugs hit:
- `scanner.close()` inside the loop crashed the REPL on the second iteration
- `Arrays.toString(args)` produces `[arg1, arg2]` as a literal string, not a valid command array
- `System.arraycopy` requires arrays; `fullCommand[0] = command` is the right way to set index 0

---

## Navigation Stages

### Stage 9: `pwd`

No subprocess. Java exposes the working directory as a system property.

```java
System.out.println(System.getProperty("user.dir"));
```

---

### Stage 10-11: `cd` (absolute and relative paths)

`cd` updates `user.dir`. Two-argument `File` constructor handles relative paths naturally.

`getCanonicalPath()` over `getAbsolutePath()`: the latter leaves `../` unresolved in the string. `getCanonicalPath()` collapses them.

The absolute path branch is necessary. Java's `new File(base, child)` does not ignore `base` when `child` is absolute -- unlike .NET. Passing `/tmp/foo` as child with a base produces `/home/user/tmp/foo`.

```java
File dir = path.startsWith("/") ? new File(path)
         : new File(System.getProperty("user.dir"), path);

if (dir.exists() && dir.isDirectory())
    System.setProperty("user.dir", dir.getCanonicalPath());
else
    System.out.println("cd: " + path + ": No such file or directory");
```

---

### Stage 12: `cd ~`

`~` expands to `$HOME`. Added as a branch before the relative path case.

Known limitation: `~/documents` is not handled -- only bare `~`. Not required by the stage.

---

## Quoting Stages

### Stage 13: Single quotes

Characters inside single quotes lose all special meaning. Spaces are preserved. Adjacent quoted segments concatenate.

Required replacing the naive `arguments.split(" ")` approach with a proper character-by-character parser. The parser also moved to accept the full input line (command included), so all builtins and externals benefit from correct tokenization.

```java
if (c == '\'') {
    i++;  // skip opening quote
    while (i < input.length() && input.charAt(i) != '\'') {
        current.append(input.charAt(i++));
    }
    i++;  // skip closing quote
}
```

Concatenation works naturally: adjacent quoted segments both append into the same `current` buffer with no space delimiter between them.

---

### Stage 14: Double quotes

Same as single quotes except backslash escapes `\"` and `\\` inside double quotes. All other backslashes inside double quotes are treated literally.

Split the quote block into two branches -- single and double -- so the backslash logic only applies inside `"..."`.

```java
if (openingQuote == '"' && input.charAt(i) == '\\') {
    char next = input.charAt(i + 1);
    if (next == '"' || next == '\\') {
        current.append(next);
        i += 2;
    } else {
        current.append(input.charAt(i++));  // literal backslash
    }
}
```

---

### Stage 15: Backslash outside quotes

Backslash outside quotes escapes the next character -- any character, including spaces. The backslash is consumed and not appended.

```java
} else if (c == '\\') {
    current.append(input.charAt(i + 1));
    i += 2;
}
```

An escaped space becomes part of the current token instead of a delimiter. This falls naturally out of the order of checks: the backslash branch runs before the space branch.

---

### Stage 16: Backslash inside single quotes

No change needed. Single quotes already treat everything literally including backslashes. The tester confirmed existing behavior was correct.

---

## Redirection Stages

### Stage 17: `>` and `1>` (stdout overwrite)

Redirection tokens are detected in the token list before dispatch, stripped from `cmdTokens`, and stored separately.

For builtins, `System.out` is swapped to a `FileOutputStream` before the switch runs, then restored after.

For externals, `ProcessBuilder.redirectOutput` handles it directly.

```java
// builtins
System.setOut(new PrintStream(new FileOutputStream(outputFile, false)));

// externals
pb.redirectOutput(ProcessBuilder.Redirect.to(new File(outputFile)));
```

---

### Stage 18: `2>` (stderr overwrite)

Same pattern as `>`. Added `errorFile` tracking alongside `outputFile`. `System.setErr` for builtins, `pb.redirectError` for externals.

Explicit `INHERIT` used for each stream independently so redirecting one does not affect the other.

---

### Stage 19: `>>` and `1>>` (stdout append)

Added `appendOutput` flag. `FileOutputStream(file, true)` for builtins, `ProcessBuilder.Redirect.appendTo` for externals.

---

### Stage 20: `2>>` (stderr append)

Same as `>>` but for stderr. Added `appendError` flag alongside `appendOutput`.

---
