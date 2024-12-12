// Main.java

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            // Specify the path to your input file
            String filePath = "example.txt";

            // Read the entire input from example.txt
            String inputCode = readInput(filePath);

            // Initialize Lexer
            Lexer lexer = new Lexer(inputCode);
            List<Token> tokens = lexer.tokenize();

            // Debug: Print Tokens (Optional)
            /*
            for (Token token : tokens) {
                System.out.println(token);
            }
            */

            // Initialize Parser
            Parser parser = new Parser(tokens);
            List<Node> statements = parser.parse();

            // Initialize Interpreter
            Interpreter interpreter = new Interpreter();

            // Execute statements
            interpreter.execute(statements);

            // Optionally, print final variables for debugging
            // interpreter.debugPrintVariables();

        } catch (RuntimeException e) {
            System.err.println("Error: " + e.getMessage());
        } catch (IOException e) {
            System.err.println("IO Error: " + e.getMessage());
        }
    }

    /**
     * Reads the entire content of the specified file and returns it as a String.
     *
     * @param filename The path to the input file.
     * @return The content of the file as a String.
     * @throws IOException If an I/O error occurs reading from the file.
     */
    private static String readInput(String filename) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filename)));
    }
}
