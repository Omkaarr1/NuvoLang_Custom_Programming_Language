import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        String filename = "example.txt";
        try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
            StringBuilder fullInput = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                fullInput.append(line).append("\n");
            }

            // DEBUG: Print the full input read from the file
            System.out.println("----DEBUG: FULL INPUT----");
            System.out.println(fullInput.toString());
            System.out.println("-------------------------");

            // Split statements while respecting braces
            List<String> statements = splitStatements(fullInput.toString());

            // DEBUG: Print the statements
            System.out.println("----DEBUG: STATEMENTS----");
            for (int i = 0; i < statements.size(); i++) {
                System.out.println("Statement " + i + ": " + statements.get(i).trim());
            }
            System.out.println("-------------------------");

            Interpreter interpreter = new Interpreter();

            for (int i = 0; i < statements.size(); i++) {
                String stmt = statements.get(i).trim();
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

    /**
     * Splits the input into statements by ';' while ignoring ';' within braces.
     */
    private static List<String> splitStatements(String input) {
        List<String> statements = new ArrayList<>();
        int braceCount = 0;
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '{') braceCount++;
            if (c == '}') braceCount--;
            if (c == ';' && braceCount == 0) {
                statements.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            statements.add(current.toString());
        }
        return statements;
    }

    // Recursive debug printing of the AST node structure
    public static void debugPrintNode(Node node, int indent) {
        if (node == null) {
            printIndent(indent);
            System.out.println("null");
            return;
        }

        if (node instanceof BinaryNode) {
            BinaryNode bn = (BinaryNode) node;
            printIndent(indent);
            System.out.println("BinaryNode: " + bn.op);
            debugPrintNode(bn.left, indent + 2);
            debugPrintNode(bn.right, indent + 2);
        } else if (node instanceof UnaryNode) {
            UnaryNode un = (UnaryNode) node;
            printIndent(indent);
            System.out.println("UnaryNode: " + un.op + " (postfix=" + un.postfix + ")");
            debugPrintNode(un.expr, indent + 2);
        } else if (node instanceof LiteralNode) {
            LiteralNode ln = (LiteralNode) node;
            printIndent(indent);
            System.out.println("LiteralNode: " + ln.value);
        } else if (node instanceof VariableNode) {
            VariableNode vn = (VariableNode) node;
            printIndent(indent);
            System.out.println("VariableNode: " + vn.name);
        } else if (node instanceof AssignNode) {
            AssignNode an = (AssignNode) node;
            printIndent(indent);
            System.out.println("AssignNode: " + an.name + " " + an.op);
            debugPrintNode(an.value, indent + 2);
        } else if (node instanceof PrintNode) {
            PrintNode pn = (PrintNode) node;
            printIndent(indent);
            System.out.println("PrintNode");
            debugPrintNode(pn.expr, indent + 2);
        } else if (node instanceof IfNode) {
            IfNode ifn = (IfNode) node;
            printIndent(indent);
            System.out.println("IfNode");
            printIndent(indent + 2);
            System.out.println("Condition:");
            debugPrintNode(ifn.condition, indent + 4);
            printIndent(indent + 2);
            System.out.println("IfBranch:");
            debugPrintNode(ifn.ifBranch, indent + 4);
            printIndent(indent + 2);
            System.out.println("ElseBranch:");
            debugPrintNode(ifn.elseBranch, indent + 4);
        } else if (node instanceof LoopNode) {
            LoopNode ln = (LoopNode) node;
            printIndent(indent);
            System.out.println("LoopNode: Loop from");
            debugPrintNode(ln.start, indent + 2);
            printIndent(indent + 2);
            System.out.println("To:");
            debugPrintNode(ln.end, indent + 4);
            printIndent(indent + 2);
            System.out.println("Body:");
            for (Node stmt : ln.body) {
                debugPrintNode(stmt, indent + 4);
            }
        } else if (node instanceof InputNode) {
            InputNode in = (InputNode) node;
            printIndent(indent);
            System.out.println("InputNode");
            printIndent(indent + 2);
            System.out.println("Prompt:");
            debugPrintNode(in.prompt, indent + 4);
            printIndent(indent + 2);
            System.out.println("Variable:");
            debugPrintNode(in.variable, indent + 4);
        } else if (node instanceof ExpressionStatement) {
            ExpressionStatement es = (ExpressionStatement) node;
            printIndent(indent);
            System.out.println("ExpressionStatement:");
            debugPrintNode(es.expr, indent + 2);
        } else {
            printIndent(indent);
            System.out.println("Unknown Node Type: " + node.getClass().getName());
        }
    }

    private static void printIndent(int indent) {
        for (int i = 0; i < indent; i++) System.out.print(" ");
    }
}
