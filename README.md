# Custom Programming Language Interpreter in Java

This project implements a **custom programming language interpreter** in Java, complete with a **lexer**, **parser**, and **interpreter**. It reads program files, tokenizes and parses them, and executes the code line-by-line. The language supports a wide range of features, including variable assignments, arithmetic operations, conditional statements (`if-else`), functions, arrays, encrypted variables, machine learning integrations, blockchain functionalities, data science operations, and temporal event triggers.

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
   - **Encrypted Variables**: Variables prefixed with `@ENC`. Their values are stored encrypted and decrypted on access.

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
   - **Encrypted Variables**: Variables starting with `@ENC` store their values in encrypted form.
   - Encryption/decryption is handled internally, invisible to the user when performing operations.
   - Computations on encrypted variables decrypt temporarily, perform the operation, then re-encrypt if needed.

10. **Temporal Programming (Event Triggers):**
    - **Unique Feature**: Allows scheduling of code execution at specific times or intervals.
    - **Syntax**:
      - `@EVENT_TRIGGER(duration,"seconds") -> <statement>;`  
        Executes the specified statement every given duration (e.g., every 5 seconds).
      - `@EVENT_TRIGGER("YYYY-MM-DD HH:MM:SS") -> <statement>;`  
        Executes the specified statement once at the exact given date and time.
    - **Use Cases**: Event-driven and time-based execution patterns.

11. **Machine Learning Integration:**
    - **ML Library**: Integrates with Weka for machine learning tasks.
    - **Supported Methods**:
      - `randomforest(csvPath, [targetColumn]);`
      - `linearregression(csvPath, [targetColumn]);`
      - `kmeans(csvPath);`
    - **Features**:
      - Load datasets from CSV files.
      - Train models like Random Forest, Linear Regression, and K-Means.
      - Evaluate model performance with metrics like accuracy, precision, recall, and F1-score.

12. **Blockchain Functionality:**
    - **Blockchain Library**: Simulates basic blockchain operations.
    - **Supported Methods**:
      - `init(privateKey, initialAmount);`
      - `transaction(toAddress, amount);`
      - `showCurrentBalance();`
      - `showTransactionHistory();`
    - **Features**:
      - Initialize blockchain with a private key and balance.
      - Perform transactions to other addresses.
      - Display current balance and transaction history.

13. **Data Science Operations:**
    - **DataScience Library**: Provides data manipulation and statistical analysis tools.
    - **Supported Methods**:
      - `loadCSV(csvPath);`
      - `calculateMean(data, attributeName);`
      - `calculateMedian(data, attributeName);`
      - `calculateStdDev(data, attributeName);`
      - `plotHistogram(data, attributeName, outputPath);`
      - `plotScatter(data, attributeX, attributeY, outputPath);`
      - `filterData(data, attribute, operator, value);`
    - **Features**:
      - Load and preprocess datasets.
      - Perform statistical calculations.
      - Generate visualizations like histograms and scatter plots.
      - Filter datasets based on specific conditions.

14. **Error Handling:**
    - **Lexer and Parser**: Report line and column numbers for syntax errors.
    - **Runtime Errors**: Produce descriptive messages (e.g., undefined variable, division by zero).

15. **Dynamic and Interpreted Nature:**
    - **Interpreted Execution**: Code is lexed, parsed, and interpreted at runtime.
    - **Dynamic Typing**: Variables and functions are dynamically created at runtime, no explicit type declarations needed.
    - **On-the-Fly Computations**: Supports dynamic computations and modifications of variables.

---

## Input File Syntax

- **Statements** must be terminated by a semicolon (`;`).
- **Output Statements**: Use `print->"text" + variable;`.
- **Input Statements**: Use `input-> "prompt" -> variable;`.
- **Conditionals**: Must be written as one statement: `if (condition) <statement> else <statement>;`.
- **Function Definitions**: Use the `function` keyword followed by the function name and parameters.
- **Event Triggers**: Use `@EVENT_TRIGGER(duration,"unit") -> <statement>;` or `@EVENT_TRIGGER("YYYY-MM-DD HH:MM:SS") -> <statement>;`.

### Example Input File (`example.txt`):
```plaintext
use blockchain;
use data_science;
use ml;
// Variable Assignments and Arithmetic Operations
x = 10;
x = x + 5;
print->"Value of x = " + x;
y = 20;
y++;
print->"Value of y after increment = " + y;
z = x * y;
print->"Value of z = " + z;

// Conditional Statement
if  (z > 100) { print->"z is greater than 100"; } else { print->"z is not greater than 100"; }

// Encrypted Variable
@ENCsecret = "mySecretValue";
print->"Encrypted secret = " + @ENCsecret;

// Event Trigger: Print "Hello" every 5 seconds

// Function Definition and Call
function add(a, b) {
  return a + b;
};

result = add(10, 20);
print->"Result of add function = " + result;

rfModel = ml.randomforest("scripts/data.csv", "class");
print->"Random Forest Model: " + rfModel;

// Blockchain Operations
blockchain.init("myPrivateKey", 1000);
blockchain.transaction("recipientAddress", 250);
blockchain.showCurrentBalance();
blockchain.showTransactionHistory();

// Data Science Operations
dataset = data_science.loadCSV("scripts/data2.csv");
meanAge = data_science.calculateMean(dataset, "age");
medianAge = data_science.calculateMedian(dataset, "age");
stdDevAge = data_science.calculateStdDev(dataset, "age");
print->"Mean age: " + meanAge;
print->"Median age: " + medianAge;
print->"Standard Deviation of age: " + stdDevAge;

// Visualization
data_science.plotHistogram(dataset, "age", "scripts/age_histogram.png");
data_science.plotScatter(dataset, "age", "salary", "scripts/age_salary_scatter.png");

// Data Filtering
filteredData = data_science.filterData(dataset, "age", ">", 30);
print->"Filtered Data Instances (age > 30): " + filteredData.numInstances();
```

### Corresponding Output:
```plaintext
Value of x = 15
Value of y after increment = 21.0
Value of z = 315
z is greater than 100
Encrypted secret = nQiw0L5BJgJBo+MsCqZPIg==
Result of add function = 30
[ml] Random Forest trained on scripts/data.csv with target column 'class'
[ml] Model Summary:
RandomForest

Bagging with 100 iterations and base learner

weka.classifiers.trees.RandomTree -K 0 -M 1.0 -V 0.001 -S 1 -do-not-check-capabilities
[ml] Accuracy: 40.0%
[ml] Class: unacc
    Precision: 1.0
    Recall: 0.5
    F1-Score: 0.6666666666666666
[ml] Class: acc
    Precision: 0.6666666666666666
    Recall: 1.0
    F1-Score: 0.8
[ml] Class: vgood
    Precision: 0.0
    Recall: 0.0
    F1-Score: 0.0
[ml] Class: good
    Precision: 0.0
    Recall: 0.0
    F1-Score: 0.0
[ml] Random Forest model saved to 'randomforest.model'
Random Forest Model: RandomForest

Bagging with 100 iterations and base learner

weka.classifiers.trees.RandomTree -K 0 -M 1.0 -V 0.001 -S 1 -do-not-check-capabilities
[blockchain] Initialized:
    Address: adbb30a8
    Balance: 1000.0
[blockchain] Transaction successful!
    hashCode: -555811334
    transactionID: eafa8cb6-9efc-499a-acdb-76716a1cca5d
    amount: 250.0
    to Address: recipientAddress
[blockchain] Current Balance: 750.0
[blockchain] Transaction History:
  Transaction 1:
    To Address: recipientAddress
    Amount: 250.0
    Transaction ID: eafa8cb6-9efc-499a-acdb-76716a1cca5d
    Hash Code: -555811334
[data science] Loaded data from scripts/data2.csv
[data science] Mean of 'age': 34.55
[data science] Median of 'age': 34.5
[data science] Standard Deviation of 'age': 6.00416522091123
Mean age: 34.55
Median age: 34.5
Standard Deviation of age: 6.00416522091123
[data science] Histogram saved to scripts/age_histogram.png
[data science] Scatter plot saved to scripts/age_salary_scatter.png
[data science] Filtered data based on age > 30.0
[data science] Number of instances after filtering: 14
Filtered Data Instances (age > 30): 14
```

*Note: The "Hello" message will continue to print every 5 seconds until the program is terminated.*

---

## Project Structure
1. **Main.java**:  
   The entry point of the application. It reads the input source code from a file (e.g., `example.txt`) and passes it through the Lexer, Parser, and Interpreter components. Main initializes these components and executes the interpreted code. It also handles exceptions that may occur during runtime or compilation.

2. **Lexer.java**:  
   Responsible for breaking down the raw input text into a sequence of tokens. It performs character-by-character analysis of the input source code, grouping characters into meaningful symbols like keywords, operators, and identifiers. The Lexer also handles whitespace, comments, and string literals. If any unrecognized or invalid characters are encountered, it reports lexical errors with line and column numbers.

3. **Token.java**:  
   Represents a single meaningful unit in the source code. Defines a `Token` class that encapsulates the type of token (e.g., identifier, keyword, operator), its lexeme (the actual string representation), and its position in the source code (line and column). These tokens are passed from the Lexer to the Parser.

4. **TokenType.java**:  
   Defines an enumeration (`enum`) of all possible token types that the language supports. Includes keywords (`PRINT`, `IF`, `ELSE`, etc.), operators (`+`, `-`, `*`, `/`, `==`, etc.), and symbols (`{`, `}`, `(`, `)`, etc.). Helps the Lexer and Parser recognize and categorize the input text.

5. **Parser.java**:  
   Takes the list of tokens produced by the Lexer and organizes them into an abstract syntax tree (AST). The AST is a structured representation of the code that reflects the logical grouping of expressions, statements, and control flow constructs. The Parser enforces the grammar of the language, such as the rules for loops, functions, and expressions. If it encounters invalid syntax, it reports errors with precise locations in the source code.

6. **Interpreter.java**:  
   Executes the AST produced by the Parser. Traverses the tree and evaluates nodes based on their type, handling variable assignments, arithmetic, functions, loops, and other constructs. Implements the language's unique features, such as encrypted variables (`@ENC`) and temporal triggers (`@EVENT_TRIGGER`). Integrates with external libraries for machine learning, blockchain, and data science operations. Maintains a runtime environment that includes variables, functions, and a call stack to support recursive function calls. Manages a time scheduler for event-driven programming.

7. **Libraries**:
   - **MlLibrary.java**:  
     Integrates with Weka to provide machine learning functionalities like Random Forest, Linear Regression, and K-Means clustering.
   
   - **BlockchainLibrary.java**:  
     Simulates basic blockchain operations, including initializing the blockchain, performing transactions, and displaying balance and transaction history.
   
   - **DataScienceLibrary.java**:  
     Offers data manipulation and statistical analysis tools, such as loading CSV files, calculating statistical measures, generating visualizations, and filtering datasets.

8. **example.txt**:  
   Serves as the input program written in the custom language. Demonstrates various features, including variable assignments, arithmetic operations, control flow, functions, encrypted variables, machine learning tasks, blockchain operations, data science manipulations, and temporal event triggers. Acts as both a test case and a showcase of the language's capabilities.

9. **Dependencies**:
   - **Weka**: For machine learning functionalities.
   - **JFreeChart**: For generating visualizations like histograms and scatter plots.
   - **Apache Commons Math**: For statistical calculations.
   - **Java Cryptography Extension (JCE)**: For encryption and decryption of variables.

---

## How to Run

### Prerequisites
- **Java Development Kit (JDK)**: Ensure JDK is installed on your machine (version 8 or higher).
- **External Libraries**:
  - **Weka**: Download the Weka library JAR file and place it in the `lib/` directory.
  - **JFreeChart**: Download the JFreeChart library JAR files and place them in the `lib/` directory.
  - **Apache Commons Math**: Download the Commons Math library JAR file and place it in the `lib/` directory.

### Steps
1. **Clone the Repository**:
   ```bash
   git clone <repository-url>
   cd <repository-folder>
   ```

2. **Setup Libraries**:
   - Create a `lib/` directory in the project root if it doesn't exist.
   - Download the following JAR files and place them in the `lib/` directory:
     - **Weka**: [Download Weka](https://www.cs.waikato.ac.nz/ml/weka/downloading.html)
     - **JFreeChart**: [Download JFreeChart](https://www.jfree.org/jfreechart/download.html)
     - **Apache Commons Math**: [Download Commons Math](https://commons.apache.org/proper/commons-math/download_math.cgi)

3. **Create Your Input File (`example.txt`)**:
   - Place your program code in the `example.txt` file located in the project root. Refer to the **Example Input File** section above for guidance.

4. **Compile the Project**:
   ```bash
   javac -cp "lib/*" -d bin src/*.java
   ```
   - **Explanation**:
     - `-cp "lib/*"`: Includes all JAR files in the `lib/` directory in the classpath.
     - `-d bin`: Specifies the output directory for compiled `.class` files.
     - `src/*.java`: Compiles all Java source files in the `src/` directory.

5. **Run the Interpreter**:
   ```bash
   java -cp "lib/*;bin" src.Main scripts/example.txt
   ```
   - **Explanation**:
     - `-cp "lib/*;bin"`: Sets the classpath to include all JARs in `lib/` and compiled classes in `bin/`.
       - **Windows Classpath Separator**: `;` (semicolon)
       - **Unix/Linux/MacOS Classpath Separator**: `:` (colon)
     - `src.Main`: Fully qualified class name (`Main` class within the `src` package).
     - `scripts/example.txt`: Path to your script file.

6. **View Outputs and Generated Files**:
   - **Console Output**: Displays the results of `print` statements and any runtime messages.
   - **Visualization Files**: Check the `scripts/` directory for generated PNG files like `age_histogram.png` and `age_salary_scatter.png`.

---

## Debugging Features

- **Token Inspection**: Displays tokens generated by the lexer.
- **AST Inspection**: Displays the parsed Abstract Syntax Tree (AST).
- **Variable States**: Prints the state of all variables after each statement.
- **Error Messages**: Provides detailed error messages with line and column numbers for syntax and runtime errors.

---

## Customization

You can extend this language by:

1. **Adding New Operators or Keywords**:
   - Update `TokenType.java` with new tokens.
   - Modify `Lexer.java` to recognize new tokens.
   - Update `Parser.java` to handle new grammar rules.
   - Implement corresponding functionalities in `Interpreter.java`.

2. **Integrating Additional Libraries**:
   - Add new libraries by creating corresponding classes and integrating them within the `Interpreter.java`.
   - Update the `loadLibrary` method to support new libraries.

3. **Enhancing Existing Features**:
   - Implement more advanced encryption methods.
   - Add support for more complex data structures.
   - Extend the temporal programming capabilities with more scheduling options.

---

## Example Enhancements

### Add Machine Learning Operations:
Support constructs like:
```plaintext
use ml;
rfModel = ml.randomforest("scripts/data2.csv", "age");
print->"Random Forest Model: " + rfModel;
```

### Add Blockchain Operations:
Support constructs like:
```plaintext
use blockchain;
blockchain.init("myPrivateKey", 1000);
blockchain.transaction("recipientAddress", 250);
blockchain.showCurrentBalance();
blockchain.showTransactionHistory();
```

### Add Data Science Operations:
Support constructs like:
```plaintext
use data_science;
dataset = data_science.loadCSV("scripts/data2.csv");
meanAge = data_science.calculateMean(dataset, "age");
medianAge = data_science.calculateMedian(dataset, "age");
stdDevAge = data_science.calculateStdDev(dataset, "age");
print->"Mean age: " + meanAge;
print->"Median age: " + medianAge;
print->"Standard Deviation of age: " + stdDevAge;

// Visualization
data_science.plotHistogram(dataset, "age", "scripts/age_histogram.png");
data_science.plotScatter(dataset, "age", "salary", "scripts/age_salary_scatter.png");

// Data Filtering
filteredData = data_science.filterData(dataset, "age", ">", 30);
print->"Filtered Data Instances (age > 30): " + filteredData.numInstances();
```

### Add Advanced Encryption Features:
Support more encryption algorithms or key management systems.

---

## Roadmap

- [x] Add support for loops (`while`, `for`).
- [x] Implement function declarations and calls.
- [x] Extend type support (e.g., boolean, floating-point).
- [x] Improve error handling with detailed messages.
- [x] Add support for arrays.
- [x] Integrate Machine Learning functionalities using Weka.
- [x] Implement Blockchain functionalities.
- [x] Incorporate Data Science operations and visualizations.
- [x] Add Encryption support for variables.
- [x] Implement Temporal Programming with event triggers.

### Future Enhancements:
- **Advanced Data Structures**: Support for objects, maps, and more complex data types.
- **Standard Library**: Implement a standard library with common utilities.
- **Concurrency Support**: Enable multi-threaded execution within the language.
- **Extended ML Models**: Add more machine learning models and evaluation metrics.
- **Persistent Storage**: Allow saving and loading of interpreter state.
- **Enhanced Security**: Implement more robust encryption and security features.

---

## Contributing

Contributions are welcome! To contribute:

1. **Fork the Repository**:
   ```bash
   git clone <repository-url>
   cd <repository-folder>
   ```

2. **Create a New Branch for Your Feature**:
   ```bash
   git checkout -b feature/YourFeatureName
   ```

3. **Commit Your Changes**:
   ```bash
   git commit -m "Add Your Feature"
   ```

4. **Push to Your Forked Repository**:
   ```bash
   git push origin feature/YourFeatureName
   ```

5. **Open a Pull Request**:
   - Navigate to the original repository on GitHub.
   - Click on "Compare & pull request" for your branch.
   - Provide a descriptive title and detailed description of your changes.
   - Submit the pull request.

---

## License

This project is licensed under the MIT License. See the `LICENSE` file for more details.

---

## Acknowledgments

- Inspired by the concepts of lexing, parsing, and interpreting as used in compiler design.
- Developed using Java for its robust features and cross-platform support.
- Utilizes powerful libraries like **Weka**, **JFreeChart**, and **Apache Commons Math** for extended functionalities.
- Special thanks to the open-source community for providing invaluable resources and tools.

---

**Happy Coding! 🚀**
