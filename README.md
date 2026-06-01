# codecrafters-shell-java

A POSIX-style shell built from scratch in Java. Follows the [codecrafters.io shell challenge](https://codecrafters.io/challenges/shell).

---

## Implementation Notes

### REPL
Input is split into `command` (first token) and `arguments` (remainder) on each iteration.

### Builtins
`exit`, `echo`, `type`, `pwd`, `cd` are handled directly in Java — no subprocess.

### `type`
Walks each directory in `$PATH` looking for a matching executable. Reports builtin, external binary, or not found.

### Running external programs
`ProcessBuilder` is used over `Runtime.exec()` for stdio control. Pass the command name as typed for `argv[0]` — not the resolved absolute path. The OS handles `$PATH` resolution itself.


### `pwd`
```java
System.out.println(System.getProperty("user.dir"));
```

### `cd`
Handles absolute paths, relative paths (`./`, `../`, subdirectories), and `~` (expands to `$HOME`).

`getCanonicalPath()` is used over `getAbsolutePath()` to resolve `../` and `./` correctly.

---

## Stack

- Java 25 (Amazon Corretto)
- Maven
- Linux (Ubuntu)

## Run locally

```bash
git clone https://github.com/your-username/codecrafters-shell-java
cd codecrafters-shell-java
mvn compile
./your_program.sh
```
