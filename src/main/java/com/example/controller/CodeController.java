// src/main/java/com/example/demospring/controller/CodeController.java

package com.example.demospring.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.controller.CodeExecutionRequest;
import com.example.demospring.dto.CodeExecutionResponse;
import com.example.lang.Lexer;
import com.example.lang.Parser;
import com.example.lang.Token;

@Controller
public class CodeController {

    /**
     * Dynamically fetch the project's path from the current working directory.
     * This avoids having a hard-coded path.
     */
    private static final String PROJECT_PATH = System.getProperty("user.dir");

    private static final String SCRIPT_PATH = "scripts" + File.separator + "example.txt";

    /**
     * Handles GET requests to the root URL and returns the index.html template.
     */
    @GetMapping("/")
    public String showForm() {
        return "index"; // Renders src/main/resources/templates/index.html
    }

    /**
     * Handles POST requests to /runCode. Processes the submitted code,
     * executes it, and returns a JSON response with the results.
     */
    @PostMapping("/runCode")
    @ResponseBody
    public ResponseEntity<CodeExecutionResponse> runCode(@RequestBody CodeExecutionRequest request) {
        CodeExecutionResponse response = new CodeExecutionResponse();
        String code = request.getCode();

        // Validate input
        if (code == null || code.trim().isEmpty()) {
            response.setMessage("Error: Code input is empty.");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        
        // Step 1: Save user's code to scripts/example.txt
        File scriptFile = new File(PROJECT_PATH, SCRIPT_PATH);
        try (FileWriter fw = new FileWriter(scriptFile)) {
            fw.write(code);
        } catch (IOException e) {
            response.setMessage("Error writing to example.txt: " + e.getMessage());
            response.setCompileOutput("");
            response.setOutput("");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        // Step 2: Tokenize and parse the code
        StringBuilder parseOutput = new StringBuilder();
        try {
            Lexer lexer = new Lexer(code);
            List<Token> tokens = lexer.tokenize();

            Parser parser = new Parser(tokens);
            String parseResult = parser.getParseResultAsString();

            parseOutput.append("--- PARSE RESULT ---\n");
            for (String line : parseResult.split("\n")) {
                parseOutput.append(formatNode(line)).append("\n");
            }
        } catch (Exception e) {
            parseOutput.append("[Parse Error] ").append(e.getMessage()).append("\n");
        }

        // Step 3: Compile the code
        // Adjust commands if necessary (e.g., for OS-specific path separators)
        String compileCommand = "javac -cp \"lib/*;bin\" -d bin src" + File.separator + "main"
                + File.separator + "java" + File.separator + "com" + File.separator + "example"
                + File.separator + "lang" + File.separator + "*.java";
        StringBuilder compileOutput = executeCommand(compileCommand, "Compilation", PROJECT_PATH);
        if (compileOutput == null) {
            response.setMessage("Compilation failed.");
            response.setCompileOutput(parseOutput.toString());
            response.setOutput("");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }

        // Step 4: Run the compiled class
        String runCommand = "java -cp \"lib/*;bin\" com.example.lang.Main " + SCRIPT_PATH.replace(File.separator, "/");
        StringBuilder runOutput = executeCommand(runCommand, "Execution", PROJECT_PATH);

        // Combine outputs and set in the response
        response.setMessage("Code executed successfully.");
        response.setCompileOutput(parseOutput.toString() + "\n" + compileOutput.toString());
        response.setOutput(formatRuntimeOutput(runOutput != null ? runOutput.toString() : "No output generated."));

        return ResponseEntity.ok(response);
    }

    /**
     * Helper method to execute a system command and capture its output.
     *
     * @param command   The command to execute.
     * @param stage     The stage name (e.g., "Compilation", "Execution") for
     *                  logging.
     * @param directory The directory to execute the command in.
     * @return The output of the command or null if the command failed.
     */
    private StringBuilder executeCommand(String command, String stage, String directory) {
        StringBuilder output = new StringBuilder();
        ProcessBuilder processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
        processBuilder.directory(new File(directory));
        processBuilder.redirectErrorStream(true);

        try {
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                output.append(stage).append(" failed with exit code ").append(exitCode).append("\n");
                return null;
            }
        } catch (IOException | InterruptedException e) {
            output.append(stage).append(" error: ").append(e.getMessage()).append("\n");
            return null;
        }

        return output;
    }

    /**
     * Formats node output to be more human-readable.
     *
     * @param node The raw node string.
     * @return The formatted node string.
     */
    private String formatNode(String node) {
        if (node.contains("@")) {
            String[] parts = node.split("@");
            return parts[0].replace("com.example.lang.", "") + " (ID: " + parts[1] + ")";
        }
        return node;
    }

    /**
     * Formats the runtime output for better readability.
     *
     * @param output The raw runtime output.
     * @return The formatted runtime output.
     */
    private String formatRuntimeOutput(String output) {
        StringBuilder formattedOutput = new StringBuilder();
        String[] lines = output.split("\n");
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                formattedOutput.append(line.trim()).append("\n");
            }
        }
        return formattedOutput.toString();
    }
}
