package com.example.demospring.controller;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class CodeController {

    // Where the user code is stored temporarily (optional).
    private static final String SCRIPT_PATH = "scripts/example.txt";

    // Update if your project folder is different.
    // This should be the directory containing:
    //  - src\Main.java
    //  - bin\
    //  - lib\
    //  - scripts\example.txt
    private static final String PROJECT_PATH = "C:\\Users\\omkar\\Desktop\\Project 2";

    @GetMapping("/")
    public String showForm(Model model) {
        // Renders src/main/resources/templates/index.html
        return "index";
    }

    @PostMapping("/runCode")
    public String runCode(@RequestParam("code") String code, Model model) {

        // 1) Write code to scripts\example.txt (optional step)
        File scriptFile = new File(PROJECT_PATH, SCRIPT_PATH);
        try (FileWriter fw = new FileWriter(scriptFile)) {
            fw.write(code);
        } catch (IOException e) {
            model.addAttribute("message", "Error writing to example.txt: " + e.getMessage());
            return "index";
        }

        // 2) Compile src\Main.java -> bin\src\Main.class
        //    using the same command you'd do manually in CMD:
        //    javac -cp "lib/*;bin" -d bin src\Main.java
        ProcessBuilder compileBuilder = new ProcessBuilder(
            "cmd.exe", "/c",
            "javac -cp \"lib/*;bin\" -d bin src\\Main.java"
        );
        // Must set working dir so paths match exactly
        compileBuilder.directory(new File(PROJECT_PATH));
        compileBuilder.redirectErrorStream(true);

        StringBuilder compileOutput = new StringBuilder();
        try {
            Process compileProcess = compileBuilder.start();
            try (BufferedReader reader = new BufferedReader(
                     new InputStreamReader(compileProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    compileOutput.append(line).append("\n");
                }
            }
            int compileExitCode = compileProcess.waitFor();
            if (compileExitCode != 0) {
                // Compilation failed
                model.addAttribute("message", "Compilation failed with exit code " + compileExitCode);
                model.addAttribute("output", compileOutput.toString());
                return "index";
            }
        } catch (IOException | InterruptedException e) {
            model.addAttribute("message", "Error compiling: " + e.getMessage());
            return "index";
        }

        // 3) Run the compiled class
        //    "java -cp \"lib/*;bin\" src.Main scripts\example.txt"
        ProcessBuilder runBuilder = new ProcessBuilder(
            "cmd.exe", "/c",
            "java -cp \"lib/*;bin\" src.Main scripts\\example.txt"
        );
        runBuilder.directory(new File(PROJECT_PATH));
        runBuilder.redirectErrorStream(true);

        StringBuilder runOutput = new StringBuilder();
        try {
            Process runProcess = runBuilder.start();
            try (BufferedReader reader = new BufferedReader(
                     new InputStreamReader(runProcess.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    runOutput.append(line).append("\n");
                }
            }
            int runExitCode = runProcess.waitFor();
            model.addAttribute("message", "Code executed with exit code " + runExitCode);

        } catch (IOException | InterruptedException e) {
            model.addAttribute("message", "Error running the command: " + e.getMessage());
        }

        // 4) Provide compilation output + runtime output to the user
        model.addAttribute("compileOutput", compileOutput.toString());
        model.addAttribute("output", runOutput.toString());

        return "index";
    }
}
