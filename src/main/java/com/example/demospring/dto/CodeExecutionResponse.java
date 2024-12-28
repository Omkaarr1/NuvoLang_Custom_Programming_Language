// src/main/java/com/example/demospring/dto/CodeExecutionResponse.java

package com.example.demospring.dto;

public class CodeExecutionResponse {
    private String message;
    private String compileOutput;
    private String output;

    // Default Constructor
    public CodeExecutionResponse() {}

    // Parameterized Constructor
    public CodeExecutionResponse(String message, String compileOutput, String output) {
        this.message = message;
        this.compileOutput = compileOutput;
        this.output = output;
    }

    // Getters and Setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCompileOutput() {
        return compileOutput;
    }

    public void setCompileOutput(String compileOutput) {
        this.compileOutput = compileOutput;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }
}
