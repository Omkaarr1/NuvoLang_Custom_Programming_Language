import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String filename = "example.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            StringBuilder fullInput = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                fullInput.append(line).append(" ");
            }

            // DEBUG: Print the full input read from the file
            System.out.println("----DEBUG: FULL INPUT----");
            System.out.println(fullInput.toString());
            System.out.println("-------------------------");

            String[] statements = fullInput.toString().split(";");
            
            // DEBUG: Print the statements
            System.out.println("----DEBUG: STATEMENTS----");
            for (int i = 0; i < statements.length; i++) {
                System.out.println("Statement " + i + ": " + statements[i].trim());
            }
            System.out.println("-------------------------");

            Interpreter interpreter = new Interpreter();

            for (int i = 0; i < statements.length; i++) {
                String stmt = statements[i].trim();
                if (stmt.isEmpty()) continue;
                try {
                    // Tokenize
                    Lexer lexer = new Lexer(stmt);
                    List<Token> tokens = lexer.tokenize();

                    // DEBUG: Print tokens
                    System.out.println("----DEBUG: TOKENS for Statement " + i + "----");
                    for (Token t : tokens) {
                        System.out.println(t);
                    }
                    System.out.println("--------------------------------");

                    // Parse
                    Parser parser = new Parser(tokens);
                    Node node = parser.parseStatement();

                    // DEBUG: Print the parsed node (structure)
                    System.out.println("----DEBUG: PARSED NODE for Statement " + i + "----");
                    debugPrintNode(node, 0);
                    System.out.println("--------------------------------");

                    // Execute
                    interpreter.execute(node);

                    // DEBUG: Print variables after executing the statement
                    interpreter.debugPrintVariables();

                } catch (Exception e) {
                    // DEBUG: Print out exceptions with stack trace
                    System.err.println("Error while processing statement " + i + ": " + stmt);
                    e.printStackTrace();
                    break;
                }
            }

            System.out.println("All statements executed successfully.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Recursive debug printing of the AST node structure
    public static void debugPrintNode(Node node, int indent) {
        if (node == null) {
            printIndent(indent);
            System.out.println("null");
            return;
        }

        if (node instanceof BinaryNode) {
            BinaryNode bn = (BinaryNode)node;
            printIndent(indent);
            System.out.println("BinaryNode: " + bn.op);
            debugPrintNode(bn.left, indent+2);
            debugPrintNode(bn.right, indent+2);
        } else if (node instanceof UnaryNode) {
            UnaryNode un = (UnaryNode)node;
            printIndent(indent);
            System.out.println("UnaryNode: " + un.op + " (postfix=" + un.postfix + ")");
            debugPrintNode(un.expr, indent+2);
        } else if (node instanceof LiteralNode) {
            LiteralNode ln = (LiteralNode)node;
            printIndent(indent);
            System.out.println("LiteralNode: " + ln.value);
        } else if (node instanceof VariableNode) {
            VariableNode vn = (VariableNode)node;
            printIndent(indent);
            System.out.println("VariableNode: " + vn.name);
        } else if (node instanceof AssignNode) {
            AssignNode an = (AssignNode)node;
            printIndent(indent);
            System.out.println("AssignNode: " + an.name + " " + an.op);
            debugPrintNode(an.value, indent+2);
        } else if (node instanceof PrintNode) {
            PrintNode pn = (PrintNode)node;
            printIndent(indent);
            System.out.println("PrintNode");
            debugPrintNode(pn.expr, indent+2);
        } else if (node instanceof IfNode) {
            IfNode ifn = (IfNode)node;
            printIndent(indent);
            System.out.println("IfNode");
            printIndent(indent+2);
            System.out.println("Condition:");
            debugPrintNode(ifn.condition, indent+4);
            printIndent(indent+2);
            System.out.println("IfBranch:");
            debugPrintNode(ifn.ifBranch, indent+4);
            printIndent(indent+2);
            System.out.println("ElseBranch:");
            debugPrintNode(ifn.elseBranch, indent+4);
        } else if (node instanceof ExpressionStatement) {
            ExpressionStatement es = (ExpressionStatement)node;
            printIndent(indent);
            System.out.println("ExpressionStatement:");
            debugPrintNode(es.expr, indent+2);
        } else {
            printIndent(indent);
            System.out.println("Unknown Node Type: " + node.getClass().getName());
        }
    }

    private static void printIndent(int indent) {
        for (int i = 0; i < indent; i++) System.out.print(" ");
    }
}
