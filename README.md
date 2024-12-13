# Custom Programming Language Interpreter in Java

This project implements a **custom programming language interpreter** in Java, complete with a **lexer**, **parser**, and **interpreter**. It reads program files, tokenizes and parses them, and executes the code line-by-line. The language supports common Java-like features such as variable assignments, arithmetic operations, conditional statements (`if-else`), and output functionality (`print`).

---

## Features

### Key Features:
- **Arithmetic Operations**: Supports `+`, `-`, `*`, `/`, `%`.
- **Logical Operations**: `&&`, `||`, `!`.
- **Comparison Operations**: `==`, `!=`, `<`, `>`, `<=`, `>=`.
- **Unary Operations**: `++`, `--` (both prefix and postfix).
- **Conditional Statements**: `if` and `if-else` constructs.
- **Output Statements**: Custom print syntax (`print->`).
- **Variable Assignments**: Including compound assignments (`+=`, `-=`, etc.).

---

## Input File Syntax

- **Statements** must be terminated by a semicolon (`;`).
- **Output Statements**: Use `print->"text" + variable;`.
- **Conditionals**: Must be written as one statement: `if (condition) <statement> else <statement>;`.

### Example Input File (`example.txt`):
```plaintext
x = 10;
x = x + 5;
print->"Value of x = " + x;
y = 20;
y++;
print->"Value of y after increment = " + y;
z = x * y;
print->"Value of z = " + z;
if (z > 100) print->"z is greater than 100" else print->"z is not greater than 100";
print->"Done";
```

### Corresponding Output:
```plaintext
Value of x = 15.0
Value of y after increment = 21.0
Value of z = 315.0
z is greater than 100
Done
```

---

## Project Structure

The interpreter comprises the following components:

1. **`Main.java`**  
   - Reads input from the `example.txt` file.
   - Splits the input into statements using the `;` delimiter.
   - Passes each statement through the **lexer**, **parser**, and **interpreter** for execution.

2. **`Token.java`**  
   - Defines the token types (`IDENTIFIER`, `NUMBER`, `STRING`, etc.).
   - Represents individual tokens generated during lexing.

3. **`Lexer.java`**  
   - Breaks input strings into tokens.
   - Handles keywords, symbols, numbers, and strings.

4. **`Parser.java`**  
   - Constructs an Abstract Syntax Tree (AST) from tokens.
   - Handles grammar rules for expressions, assignments, conditionals, and more.

5. **`Interpreter.java`**  
   - Executes the AST.
   - Maintains a variable map for runtime values.
   - Evaluates expressions and executes statements.

---

## How to Run

### Prerequisites
- **Java Development Kit (JDK)**: Ensure JDK is installed on your machine.

### Steps
1. Clone the repository:
   ```bash
   git clone <repository-url>
   cd <repository-folder>
   ```
2. Create your input file (`example.txt`) in the project directory.
3. Compile the project:
   ```bash
   javac *.java
   ```
4. Run the program:
   ```bash
   java Main
   ```
5. The program reads the `example.txt` file, executes the code, and prints the output to the console.

---

## Debugging Features

- **Token Inspection**: Displays tokens generated by the lexer.
- **AST Inspection**: Displays the parsed Abstract Syntax Tree (AST).
- **Variable States**: Prints the state of all variables after each statement.

--

## Customization

You can extend this language by:
1. Adding new operators or keywords in `TokenType` (e.g., `FOR`, `WHILE`).
2. Updating `Parser` to handle new grammar rules.
3. Modifying `Interpreter` to execute new constructs.

---

## Example Enhancements

### Add Loops:
Support constructs like:
```plaintext
for (x = 0; x < 10; x++) {
  print->"x = " + x;
}
```

### Add Functions:
Support custom functions like:
```plaintext
function add(a, b) {
  return a + b;
}
result = add(10, 20);
```

---

## Roadmap

- [x] Add support for loops (`while`, `for`).
- [x] Implement function declarations and calls.
- [ ] Extend type support (e.g., boolean, floating-point).
- [ ] Improve error handling with detailed messages.
- [ ] Add support for arrays and lists.

---

## Contributing

Contributions are welcome! To contribute:
1. Fork the repository.
2. Create a new branch for your feature.
3. Commit your changes and open a pull request.

---

## License

This project is licensed under the MIT License. See the `LICENSE` file for more details.

---

## Acknowledgments

- Inspired by the concepts of lexing, parsing, and interpreting as used in compiler design.
- Developed using Java for its robust features and cross-platform support.
