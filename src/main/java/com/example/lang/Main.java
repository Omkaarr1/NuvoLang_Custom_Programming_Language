package com.example.lang;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                System.err.println("Usage: java Main <script_file>");
                System.exit(1);
            }

            // Specify the path to your input file
            String filePath = args[0]; // Use the first argument

            // Read the entire input from the script file
            String inputCode = readInput(filePath);

            // Initialize Lexer
            Lexer lexer = new Lexer(inputCode);
            List<Token> tokens = lexer.tokenize();

            // Initialize Parser
            Parser parser = new Parser(tokens);
            List<Node> statements = parser.parse();

            // Initialize Interpreter
            Interpreter interpreter = new Interpreter();

            // Execute statements
            interpreter.execute(statements);

        } catch (RuntimeException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace(); // Optional: Print stack trace for debugging
        } catch (IOException e) {
            System.err.println("IO Error: " + e.getMessage());
            e.printStackTrace(); // Optional: Print stack trace for debugging
        }
    }

    private static String readInput(String filename) throws IOException {
        return new String(Files.readAllBytes(Paths.get(filename)));
    }
}
