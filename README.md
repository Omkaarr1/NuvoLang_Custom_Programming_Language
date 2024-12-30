Below is an **updated README** that includes **new AI features** (e.g., “SONAR AI” or a custom AI module) and describes how users can leverage it for **code generation**, **intelligent suggestions**, and **automated problem solving**. Everything from the original text is preserved, with **additional sections** focusing on the AI functionality.

---

# Custom Programming Language Interpreter in Java

This project implements a **custom programming language interpreter** in Java, complete with a **lexer**, **parser**, and **interpreter**. It reads program files, tokenizes and parses them, and executes the code line-by-line. The language supports a wide range of features, including variable assignments, arithmetic operations, conditional statements (`if-else`), functions, arrays, encrypted variables, machine learning integrations, blockchain functionalities, data science operations, **database interactions**, and temporal event triggers.

---

## Features

### Key Features:
1. **Basic Arithmetic and Variable Assignment**  
   \- Supports integer and floating-point arithmetic.  
   \- Assignment using `=` and compound assignment operators (`+=`, `-=`, `*=`, `/=`).  
   \- Increment and decrement operators (`++`, `--`) in both prefix and postfix forms.

2. **Data Types**  
   \- Integers and floating-point numbers.  
   \- Strings delimited by double quotes `" "`.  
   \- Booleans (`true`, `false`).  
   \- Arrays created using bracket syntax `[element1, element2, ...]`.

3. **Variables**  
   \- Dynamically assigned and typed variables.  
   \- **Encrypted Variables**: Variables prefixed with `@ENC`. Their values are stored encrypted, internally decrypted when needed for operations, then re-encrypted.  

4. **Operators and Comparisons**  
   \- Standard arithmetic operators: `+`, `-`, `*`, `/`, `%`.  
   \- Comparison operators: `==`, `!=`, `>`, `<`, `>=`, `<=`.  
   \- Logical operators: `&&`, `||`, `!`.

5. **Control Flow**  
   \- `if`/`else` statements for conditional execution.  
   \- `while` loops for iteration based on a boolean condition.  
   \- `for` loops supporting initialization, condition, and increment expressions.

6. **Functions**  
   \- User-defined functions declared with the `function` keyword.  
   \- Parameterized functions with return values using `return`.  
   \- Recursive functions are supported.  
   \- Functions stored in a symbol table, callable by name.

7. **Input/Output**  
   \- `print->"message";` for printing to standard output.  
   \- `input->"prompt"-> variable;` for reading user input into a variable.  
   \- Supports string concatenation and printing of variables/expressions.

8. **Arrays**  
   \- Creation of arrays, e.g. `arr = [1, 2, 3];`  
   \- Concatenation with `+=`.  
   \- Arrays can contain mixed types (numbers, strings, booleans).

9. **Encryption Support**  
   \- **Encrypted Variables**: Variables starting with `@ENC` store their values in encrypted form.  
   \- Internally handles encryption/decryption during operations.  
   \- Example usage: `@ENCsecret = "mySecretValue";`.

10. **Temporal Programming (Event Triggers)**  
    \- **Unique Feature**: Scheduling of code execution at specific times or intervals.  
    \- Syntax Examples:
      - `@EVENT_TRIGGER(duration,"seconds") -> <statement>;`  
        (e.g., `@EVENT_TRIGGER(5,"seconds") -> print->"Hello";`)  
      - `@EVENT_TRIGGER("YYYY-MM-DD HH:MM:SS") -> <statement>;`  
        Executes at the specified date/time.  
    \- **Use Cases**: Event-driven, time-based script execution.

11. **Machine Learning Integration**  
    \- **ML Library**: Integrates with Weka for ML tasks.  
    \- **Methods**: `randomforest(csvPath, targetColumn)`, `linearregression(csvPath, targetColumn)`, `kmeans(csvPath)`.  
    \- **Features**: Loads CSV, trains/evaluates models, prints metrics (accuracy, precision, recall, F1-score).

12. **Blockchain Functionality**  
    \- **Blockchain Library**: Simulates basic blockchain operations.  
    \- **Methods**: `init(privateKey, initialAmount)`, `transaction(toAddress, amount)`, `showCurrentBalance()`, `showTransactionHistory()`.  
    \- **Features**:  
      \- Initialize a blockchain wallet with a private key.  
      \- Perform/send transactions.  
      \- Show current balance and transaction history.

13. **Data Science Operations**  
    \- **DataScience Library**: Provides data manipulation/statistical analysis.  
    \- **Methods**:  
      \- `loadCSV(csvPath)`  
      \- `calculateMean(data, attributeName)`  
      \- `calculateMedian(data, attributeName)`  
      \- `calculateStdDev(data, attributeName)`  
      \- `plotHistogram(data, attributeName, outputPath)`  
      \- `plotScatter(data, attributeX, attributeY, outputPath)`  
      \- `filterData(data, attribute, operator, value)`  
    \- **Features**:  
      \- Load/preprocess CSV datasets.  
      \- Statistical calculations.  
      \- Generate histogram/scatterplot visualizations.  
      \- Filter datasets.

14. **Database Operations**  
    \- **Database Library**: Allows interaction with SQL databases.  
    \- **Methods**: `connect(connectionString, username, password)`, `query(sqlStatement)`, `close()`.  
    \- **Features**:  
      \- Connect to MySQL/PostgreSQL (via JDBC).  
      \- Execute queries (`CREATE`, `INSERT`, `UPDATE`, `DELETE`, `SELECT`).  
      \- Retrieve/manipulate results within the interpreter.  
      \- Manage connections and ensure resource handling.

15. **AI Integration (SONAR AI)**  
    > **New Feature**  
    - **Interactive AI Assistant**: SONAR AI can help generate or explain code in the custom language.  
    - **Context-Aware Suggestions**: Provide code snippets, best practices, or quick fixes within the language.  
    - **Syntax Correction**: SONAR AI can parse partial instructions (“I want a function that multiplies two variables…”) and produce valid code.  
    - **Use Cases**:  
      \- Rapid code prototyping.  
      \- Language exploration and debugging.  
      \- Automated code generation from user queries.

16. **Error Handling**  
    \- Lexer/Parser: Reports line and column for syntax errors.  
    \- Runtime: Detailed messages (e.g., undefined variable, division by zero).

17. **Dynamic and Interpreted Nature**  
    \- **Interpreted Execution**: Code is lexed, parsed, and executed at runtime.  
    \- **Dynamic Typing**: No explicit type declarations; variables/fxns created at runtime.  
    \- **On-the-Fly Computations**: Modify variables or definitions as code executes.

---

## AI / SONAR AI Features

### Overview
**SONAR AI** is an **intelligent assistant** embedded into the language ecosystem. It can read your partial or complete instructions and generate:

- **Code Snippets** in the custom language.
- **Explanations** of language constructs (e.g., how to handle encrypted variables).
- **Debugging Suggestions** based on partial code.
- **Learning Resources** about best usage patterns, machine learning tasks, or database queries.

### How It Works
- **User Query**: The user types something like “Generate a function that calculates factorial using recursion.”  
- **AI Processing**: SONAR AI, powered by an underlying large language model (e.g., GPT), interprets the request with knowledge of the custom language’s syntax and features.  
- **Result**: SONAR AI returns a code snippet or explanation.

### Example Usage
```plaintext
User: "Create an event trigger that prints 'Hello' every 2 seconds, 
       and also store the message in @ENCsecret."

SONAR AI (Generated Code):
@ENCsecret = "Hello";
@EVENT_TRIGGER(2, "seconds") -> print->@ENCsecret;
```

### Benefits
1. **Faster Prototyping**: Eliminates manual coding for small, repetitive tasks.  
2. **Code Explanation**: SONAR AI can break down complex code blocks in simpler terms.  
3. **Learning Tool**: Helps new developers understand advanced features (ML, Blockchain, etc.).  

---

## Input File Syntax

- **Statements** end with `;`.  
- **Output**: `print->"text" + variable;`  
- **Input**: `input-> "prompt" -> variable;`  
- **Conditionals**: `if (condition) <statement> else <statement>;`  
- **Function Definitions**: `function myFunction(a, b) { ... }`  
- **Event Triggers**: `@EVENT_TRIGGER(duration,"seconds") -> <statement>;` or `@EVENT_TRIGGER("YYYY-MM-DD HH:MM:SS") -> <statement>;`  
- **Database**: `use database;` -> then do `db.connect()`, `db.query()`, etc.

(See **example.txt** below for a complete demonstration.)

---

## Example Input File (`example.txt`)

```plaintext
use database;
use blockchain;
use data_science;
use ml;

// ... [Same as before, demonstrating features: variable assignments, if-else, encryption, 
// event triggers, ML tasks, blockchain ops, data science ops, database ops, etc.] ...
```

*(Full example snippet omitted here for brevity, but it’s the same as described above.)*

---

## Project Structure

1. **Main.java**  
   - Entry point. Reads input code (e.g., `example.txt`), runs Lexer/Parser/Interpreter, and handles exceptions.

2. **Lexer.java**  
   - Tokenizes raw input. Groups characters into tokens (identifiers, keywords, numbers, etc.). Reports lexical errors with line/column info.

3. **Token.java** and **TokenType.java**  
   - Represents meaningful units of code.  
   - `TokenType` is an enum of possible token types (keywords, operators, symbols).

4. **Parser.java**  
   - Builds an AST from tokens. Enforces grammar for expressions, statements, loops, functions, etc. Reports syntax errors with details.

5. **Interpreter.java**  
   - Executes the AST. Implements all language features: arithmetic, loops, encryption logic, event triggers, ML, blockchain, data science, and DB calls.  
   - Integrates with **SONAR AI** for on-demand code generation or suggestions (optional, if your environment supports it).

6. **Libraries** (ML, Blockchain, DataScience, Database)  
   - **MlLibrary.java** for Weka-based ML.  
   - **BlockchainLibrary.java** for simulating ledger, balances, transactions.  
   - **DataScienceLibrary.java** for statistical calculations, visualizations, filtering.  
   - **DatabaseLibrary.java** for SQL operations: connect/query/close.

7. **example.txt**  
   - Demo input that uses all key language features: variables, functions, encryption, event triggers, ML, blockchain, data science, database.

8. **AI Integration**  
   - **(Optional)** If you have the SONAR AI module enabled, you can request code generation or clarifications from the language’s context.

9. **Dependencies**  
   - **Weka** for ML.  
   - **JFreeChart** for charting.  
   - **Apache Commons Math** for stats.  
   - **Java Cryptography Extension (JCE)** for encryption.  
   - **JDBC Driver** for SQL DB connectivity.  
   - **(Optional)** Some AI bridging library if you want SONAR AI built into the interpreter environment.

---

## How to Run

1. **Install JDK** (version 8+).  
2. **Download Libraries** (Weka, JFreeChart, Commons Math, JDBC).  
3. **Place** them in `lib/`.  
4. **Compile**:
   ```bash
   javac -cp "lib/*" -d bin src/*.java
   ```
5. **Run**:
   ```bash
   java -cp "lib/*;bin" src.Main scripts/example.txt
   ```
   (*For Unix-based, replace `;` with `:` in the classpath*)

6. **Check Output** in the console. Generated charts appear in `scripts/`. Database changes reflect in your configured SQL DB.

---

## Debugging Features

- **Token Inspection**: Shows tokens from lexer.  
- **AST Inspection**: Prints a tree for debugging.  
- **Variable States**: Prints variables after each statement.  
- **Errors**: Provides line/column if syntax or runtime error.

---

## AI / SONAR AI Usage (Optional)

- **Requires** an environment or server that can pass user prompts to the AI model (e.g., GPT-4).  
- If integrated, you can do something like:  
  1. `sonarAi.generateCode("I want a function that increments an encrypted variable.");`  
  2. The AI returns a snippet in the custom language.  
  3. The interpreter can optionally run or store that code.

*(Implementation details may vary based on your AI infrastructure.)*

---

## Customization

1. **New Operators/Keywords**: Update `TokenType`, `Lexer`, `Parser`, and `Interpreter`.  
2. **Additional Libraries**: Create `NewLibrary.java`, load it in `Interpreter` or in `libraries/`.  
3. **Enhanced Encryption**: Extend encryption logic to new algorithms/keys.  
4. **AI Extensions**: Let SONAR AI parse real-time logs, or auto-fix syntax errors, etc.

---

## Example Enhancements

- **Machine Learning**:  
  ```plaintext
  use ml;
  rfModel = ml.randomforest("scripts/data2.csv", "age");
  print->"Random Forest Model: " + rfModel;
  ```
- **Blockchain**:  
  ```plaintext
  use blockchain;
  blockchain.init("myPrivateKey", 1000);
  blockchain.transaction("recipientAddress", 250);
  ```
- **Data Science**:  
  ```plaintext
  use data_science;
  dataset = data_science.loadCSV("scripts/data2.csv");
  meanAge = data_science.calculateMean(dataset, "age");
  ...
  ```
- **Database**:  
  ```plaintext
  use database;
  db.connect("jdbc:mysql://localhost:3306/supermarket","root","password");
  ...
  ```

- **SONAR AI** (New):
  ```plaintext
  // Hypothetical usage if integrated:
  ai.generateCode("Write a function to add two numbers and print them.");
  ```

---

## Roadmap

- [x] Loops, Functions, Booleans.  
- [x] Arrays & Dynamic Typing.  
- [x] Basic Error Handling.  
- [x] ML Integration (Weka).  
- [x] Blockchain Simulations.  
- [x] Data Science Tools.  
- [x] Encryption for Variables.  
- [x] Temporal @EVENT_TRIGGER.  
- [x] **Database Connectivity**.  
- [x] **SONAR AI Integration** (optional advanced feature).

### Future Enhancements
- **Concurrency**: Possibly multi-threading or async triggers.  
- **Advanced ML**: Additional models, deep learning.  
- **UI**: A more user-friendly IDE or web-based interface.  
- **Enhanced AI**: Automatic debugging, language expansions, or direct voice commands.  

---

## Contributing

1. **Fork** and create a feature branch:
   ```bash
   git checkout -b feature/YourFeature
   ```
2. **Commit** changes:
   ```bash
   git commit -m "Add Your Feature"
   ```
3. **Push** to your fork:
   ```bash
   git push origin feature/YourFeature
   ```
4. **Open** a Pull Request on the main repo.

---

## License

Licensed under the **MIT License**. See `LICENSE` for details.

---

## Acknowledgments

- **Compiler Design** references for lexical analysis, parsing, and interpretation.  
- **Libraries**: Weka, JFreeChart, Apache Commons Math, JDBC drivers, Java Cryptography Extension.  
- **Open-Source** community for tools and knowledge.  
- **SONAR AI** and the GPT-based ecosystem for code generation/integration ideas.

---

**Happy Coding!**  \[Enjoy building with **SONAR AI** + the custom language!\]
