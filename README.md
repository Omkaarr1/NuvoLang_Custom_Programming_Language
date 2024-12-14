# Custom Programming Language Interpreter in Java

This project implements a **custom programming language interpreter** in Java, complete with a **lexer**, **parser**, and **interpreter**. It reads program files, tokenizes and parses them, and executes the code line-by-line. The language supports common Java-like features such as variable assignments, arithmetic operations, conditional statements (`if-else`), and output functionality (`print`).

---

## Features

### Key Features:
1. **Basic Arithmetic and Variable Assignment:**
   - Supports integer and floating-point arithmetic.
   - Assignment using `=` and compound assignment operators (`+=`, `-=`, `*=`, `/=`).
   - Increment and decrement operators (`++`, `--`) in both prefix and postfix forms.

2. **Data Types:**
   - Integers and floating-point numbers.
   - Strings delimited by double quotes `" "`.
   - Booleans (`true`, `false`).
   - Arrays created using bracket syntax `[element1, element2, ...]`.

3. **Variables:**
   - Dynamically assigned and typed variables.
   - Special encrypted variables prefixed with `@ENC`. Their values are stored encrypted and decrypted on access.

4. **Operators and Comparisons:**
   - Standard arithmetic operators: `+`, `-`, `*`, `/`, `%`.
   - Comparison operators: `==`, `!=`, `>`, `<`, `>=`, `<=`.
   - Logical operators: `&&` (AND), `||` (OR), `!` (NOT).

5. **Control Flow:**
   - `if`/`else` statements for conditional execution.
   - `while` loops for iteration based on a boolean condition.
   - `for` loops supporting initialization, condition, and increment expressions.

6. **Functions:**
   - User-defined functions declared with the `function` keyword.
   - Parameterized functions with return values using the `return` statement.
   - Recursive functions are supported.
   - Functions are first-class and stored in a symbol table, callable by name.

7. **Input/Output:**
   - `print-> "message";` for printing to standard output.
   - `input-> "prompt" -> variable;` for reading user input and storing it in a variable.
   - Supports string concatenation and printing of variables and expressions.

8. **Arrays:**
   - Creation of arrays: `arr = [1, 2, 3];`
   - Concatenation of arrays with `+=`.
   - Arrays can contain mixed types (numbers, strings, booleans).

9. **Encryption Support:**
   - Variables starting with `@ENC` store their values in encrypted form.
   - Encryption/decryption is handled internally, invisible to the user when performing operations.
   - Computations on encrypted variables decrypt temporarily, perform the operation, then re-encrypt if needed.

10. **Temporal Programming (Event Triggers):**
    - Unique feature allowing scheduling of code execution at specific times or intervals.
    - Syntax:  
      - `@EVENT_TRIGGER(duration,"seconds") -> print->"Hello";`  
        Executes the specified statement every given duration (e.g., every 5 seconds).
      - `@EVENT_TRIGGER("YYYY-MM-DD HH:MM:SS") -> print->"Happy New Year!";`  
        Executes the specified statement once at the exact given date and time.
    - Uses a time scheduler to trigger events in the future, enabling event-driven and time-based execution patterns.

11. **Error Handling:**
    - Lexer and parser report line and column numbers for syntax errors.
    - Runtime errors (e.g., undefined variable, division by zero) produce descriptive messages.

12. **Dynamic and Interpreted Nature:**
    - Code is lexed, parsed, and interpreted at runtime.
    - Variables and functions are dynamically created at runtime, no explicit type declarations needed.
    - Supports on-the-fly computations and modifications of variables.
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
1. **Main.java**:  
   This is the entry point of the application. It is responsible for reading the input source code from a file (e.g., `example.txt`) and passing it through the various stages of the interpreter. Main initializes the Lexer, Parser, and Interpreter components and then executes the interpreted code. It also handles exceptions that may occur during the runtime or compilation phases.

2. **Lexer.java**:  
   The Lexer is responsible for breaking down the raw input text into a sequence of tokens. It performs a character-by-character analysis of the input source code, grouping characters into meaningful symbols like keywords, operators, and identifiers. The Lexer also handles whitespace, comments, and string literals. If any unrecognized or invalid characters are encountered, it reports lexical errors with line and column numbers.

3. **Token.java**:  
   A token represents a single meaningful unit in the source code. This file defines a `Token` class that encapsulates the type of token (e.g., identifier, keyword, operator), its lexeme (the actual string representation), and its position in the source code (line and column). These tokens are passed from the Lexer to the Parser.

4. **TokenType.java**:  
   This file defines an enumeration (`enum`) of all possible token types that the language supports. It includes keywords (`PRINT`, `IF`, `ELSE`, etc.), operators (`+`, `-`, `*`, `/`, `==`, etc.), and symbols (`{`, `}`, `(`, `)`, etc.). It helps the Lexer and Parser recognize and categorize the input text.

5. **Parser.java**:  
   The Parser takes the list of tokens produced by the Lexer and organizes them into an abstract syntax tree (AST). The AST is a structured representation of the code that reflects the logical grouping of expressions, statements, and control flow constructs. The Parser enforces the grammar of the language, such as the rules for loops, functions, and expressions. If it encounters invalid syntax, it reports errors with precise locations in the source code.

6. **Interpreter.java**:  
   The Interpreter executes the AST produced by the Parser. It traverses the tree and evaluates nodes based on their type, handling variable assignments, arithmetic, functions, loops, and other constructs. This file also implements the language's unique features, such as encrypted variables (`@ENC`) and temporal triggers (`@EVENT_TRIGGER`). The Interpreter maintains a runtime environment that includes variables, functions, and a call stack to support recursive function calls. It also manages a time scheduler for event-driven programming.

7. **example.txt**:  
   This file serves as the input program written in the custom language. It demonstrates various features, including variable assignments, arithmetic operations, control flow, functions, array manipulations, and time-based event triggers. It acts as both a test case and a showcase of the language's capabilities.

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
- [x] Extend type support (e.g., boolean, floating-point).
- [x] Improve error handling with detailed messages.
- [x] Add support for arrays.

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
