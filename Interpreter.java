// Interpreter.java

import java.util.*;

class ReturnException extends RuntimeException {
    Object value;
    ReturnException(Object value) {
        this.value = value;
    }
}

class Interpreter {
    private Map<String, FunctionDefNode> functions = new HashMap<>();
    private Deque<Map<String, Object>> callStack = new ArrayDeque<>();
    private Scanner scanner = new Scanner(System.in);

    public Interpreter() {
        // Initialize global scope
        callStack.push(new HashMap<>());
    }

    public void execute(List<Node> nodes) {
        try {
            for (Node node : nodes) {
                executeNode(node);
            }
        } catch (ReturnException re) {
            System.err.println("Return statement outside of function.");
            throw re;
        }
    }

    private void executeNode(Node node) {
        if (node instanceof PrintNode) {
            Object val = evaluate(((PrintNode) node).expr);
            System.out.println(val);
        } else if (node instanceof IfNode) {
            IfNode ifNode = (IfNode) node;
            Object condition = evaluate(ifNode.condition);
            if (isTrue(condition)) {
                executeBlock(ifNode.ifBranch);
            } else if (ifNode.elseBranch != null) {
                executeBlock(ifNode.elseBranch);
            }
        } else if (node instanceof LoopNode) {
            LoopNode loop = (LoopNode) node;
            Object startObj = evaluate(loop.start);
            Object endObj = evaluate(loop.end);
            int start = toInteger(startObj);
            int end = toInteger(endObj);
            for (int i = start; i <= end; i++) {
                // Optionally, set a loop variable (e.g., i)
                setVariable("i", i);
                executeBlock(loop.body);
            }
        } else if (node instanceof WhileNode) {  // Handle WhileNode
            WhileNode whileNode = (WhileNode) node;
            while (isTrue(evaluate(whileNode.condition))) {
                executeBlock(whileNode.body);
            }
        } else if (node instanceof InputNode) {
            InputNode inputNode = (InputNode) node;
            Object promptObj = evaluate(inputNode.prompt);
            if (!(promptObj instanceof String)) {
                throw new RuntimeException("Input prompt must be a string.");
            }
            String prompt = (String) promptObj;
            System.out.print(prompt + " ");
            String userInput = scanner.nextLine();
            if (!(inputNode.variable instanceof VariableNode)) {
                throw new RuntimeException("Input must be assigned to a variable.");
            }
            String varName = ((VariableNode) inputNode.variable).name;
            // Attempt to parse input as number, else store as string
            Object value;
            try {
                if (userInput.contains(".")) {
                    value = Double.parseDouble(userInput);
                } else {
                    value = Integer.parseInt(userInput);
                }
            } catch (NumberFormatException e) {
                value = userInput;
            }
            setVariable(varName, value);
        } else if (node instanceof ExpressionStatement) {
            // Evaluate the expression statement, which may include assignments.
            evaluate(((ExpressionStatement) node).expr);
        } else if (node instanceof FunctionDefNode) {
            FunctionDefNode func = (FunctionDefNode) node;
            functions.put(func.name, func);
        } else if (node instanceof ReturnNode) {
            ReturnNode ret = (ReturnNode) node;
            Object value = ret.value != null ? evaluate(ret.value) : null;
            throw new ReturnException(value);
        } else {
            throw new RuntimeException("Unknown node type: " + node.getClass().getName());
        }
    }

    private void executeBlock(List<Node> statements) {
        for (Node stmt : statements) {
            executeNode(stmt);
        }
    }

    public void debugPrintVariables() {
        System.out.println("----DEBUG: CURRENT VARIABLES----");
        Map<String, Object> variables = callStack.peek();
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            System.out.println(entry.getKey() + " = " + entry.getValue());
        }
        System.out.println("--------------------------------");
    }

    private TokenType operatorFromCompound(TokenType type) {
        switch (type) {
            case PLUS_EQ:
                return TokenType.PLUS;
            case MINUS_EQ:
                return TokenType.MINUS;
            case STAR_EQ:
                return TokenType.STAR;
            case SLASH_EQ:
                return TokenType.SLASH;
            default:
                throw new RuntimeException("Unsupported compound assignment operator: " + type);
        }
    }

    private Object evaluate(Node node) {
        if (node instanceof LiteralNode) {
            return ((LiteralNode) node).value;
        } else if (node instanceof VariableNode) {
            String name = ((VariableNode) node).name;
            if (!getVariable(name).isPresent()) {
                System.err.println("----DEBUG ERROR----");
                System.err.println("Undefined variable access: " + name);
                System.err.println("------------------");
                throw new RuntimeException("Undefined variable: " + name);
            }
            return getVariable(name).get();
        } else if (node instanceof AssignNode) {
            AssignNode assign = (AssignNode) node;
            Object rightVal = evaluate(assign.value);
            Optional<Object> leftValOpt = getVariable(assign.name);
            Object leftVal = leftValOpt.orElse(null);

            if (assign.op == TokenType.ASSIGN) {
                // Simple assignment
                setVariable(assign.name, rightVal);
                return rightVal;
            } else {
                // Compound assignment (x += 5, etc.)
                if (leftVal == null) {
                    System.err.println("----DEBUG ERROR----");
                    System.err.println("Attempting compound assignment on undefined variable: " + assign.name);
                    System.err.println("------------------");
                    throw new RuntimeException("Undefined variable: " + assign.name);
                }
                Object newVal = applyOp(leftVal, rightVal, operatorFromCompound(assign.op));
                setVariable(assign.name, newVal);
                return newVal;
            }
        } else if (node instanceof BinaryNode) {
            Object left = evaluate(((BinaryNode) node).left);
            Object right = evaluate(((BinaryNode) node).right);
            return applyOp(left, right, ((BinaryNode) node).op);
        } else if (node instanceof UnaryNode) {
            UnaryNode un = (UnaryNode) node;
            if (un.postfix) {
                if (!(un.expr instanceof VariableNode)) {
                    throw new RuntimeException("Postfix operator requires a variable.");
                }
                String varName = ((VariableNode) un.expr).name;
                Optional<Object> valOpt = getVariable(varName);
                Object val = valOpt.orElse(null);
                if (val == null) {
                    System.err.println("----DEBUG ERROR----");
                    System.err.println("Undefined variable in postfix operation: " + varName);
                    System.err.println("------------------");
                    throw new RuntimeException("Undefined variable: " + varName);
                }
                Object retVal = val;
                Object newVal = applyUnary(val, un.op);
                setVariable(varName, newVal);
                return retVal;
            } else {
                if (un.op == TokenType.PLUS_PLUS || un.op == TokenType.MINUS_MINUS) {
                    if (!(un.expr instanceof VariableNode)) {
                        throw new RuntimeException("Prefix operator requires a variable.");
                    }
                    String varName = ((VariableNode) un.expr).name;
                    Optional<Object> valOpt = getVariable(varName);
                    Object val = valOpt.orElse(null);
                    if (val == null) {
                        System.err.println("----DEBUG ERROR----");
                        System.err.println("Undefined variable in prefix operation: " + varName);
                        System.err.println("------------------");
                        throw new RuntimeException("Undefined variable: " + varName);
                    }
                    Object newVal = applyUnary(val, un.op);
                    setVariable(varName, newVal);
                    return newVal;
                } else if (un.op == TokenType.NOT) {
                    Object val = evaluate(un.expr);
                    return !isTrue(val);
                }
            }
        } else if (node instanceof FunctionCallNode) {
            FunctionCallNode call = (FunctionCallNode) node;
            FunctionDefNode func = functions.get(call.name);
            if (func == null) {
                throw new RuntimeException("Undefined function: " + call.name);
            }
            if (call.arguments.size() != func.parameters.size()) {
                throw new RuntimeException("Function " + call.name + " expects " + func.parameters.size() + " arguments but got " + call.arguments.size());
            }

            // Evaluate arguments
            List<Object> argValues = new ArrayList<>();
            for (Node arg : call.arguments) {
                argValues.add(evaluate(arg));
            }

            // Create a new scope for function
            Map<String, Object> localScope = new HashMap<>();
            for (int i = 0; i < func.parameters.size(); i++) {
                localScope.put(func.parameters.get(i), argValues.get(i));
            }
            callStack.push(localScope);

            try {
                executeBlock(func.body);
            } catch (ReturnException re) {
                callStack.pop();
                return re.value;
            }

            callStack.pop();
            return null;
        } else if (node instanceof FunctionDefNode) {
            // Already handled in executeNode
            return null;
        } else if (node instanceof ReturnNode) {
            ReturnNode ret = (ReturnNode) node;
            Object value = ret.value != null ? evaluate(ret.value) : null;
            throw new ReturnException(value);
        }

        throw new RuntimeException("Unknown node type: " + node.getClass().getName());
    }

    private Object applyUnary(Object val, TokenType op) {
        double num = toNumber(val);
        switch (op) {
            case PLUS_PLUS:
                return num + 1;
            case MINUS_MINUS:
                return num - 1;
            default:
                throw new RuntimeException("Unsupported unary operator");
        }
    }

    private Object applyOp(Object left, Object right, TokenType op) {
        if (op == TokenType.PLUS && (left instanceof String || right instanceof String)) {
            return String.valueOf(left) + String.valueOf(right);
        }

        double l = toNumber(left);
        double r = toNumber(right);

        switch (op) {
            case PLUS:
                return isInteger(l) && isInteger(r) ? (int) (l + r) : (l + r);
            case MINUS:
                return isInteger(l) && isInteger(r) ? (int) (l - r) : (l - r);
            case STAR:
                return isInteger(l) && isInteger(r) ? (int) (l * r) : (l * r);
            case SLASH:
                if (r == 0) {
                    System.err.println("----DEBUG ERROR----");
                    System.err.println("Division by zero");
                    System.err.println("------------------");
                    throw new RuntimeException("Division by zero");
                }
                return isInteger(l) && isInteger(r) ? (int) (l / r) : (l / r);
            case MOD:
                if (r == 0) {
                    System.err.println("----DEBUG ERROR----");
                    System.err.println("Division by zero in modulo");
                    System.err.println("------------------");
                    throw new RuntimeException("Division by zero");
                }
                return (int) l % (int) r;
            case EQ_EQ:
                return equalsValue(l, r);
            case NOT_EQ:
                return !equalsValue(l, r);
            case GT:
                return l > r;
            case LT:
                return l < r;
            case GT_EQ:
                return l >= r;
            case LT_EQ:
                return l <= r;
            case AND_AND:
                return isTrue(left) && isTrue(right);
            case OR_OR:
                return isTrue(left) || isTrue(right);
            default:
                System.err.println("----DEBUG ERROR----");
                System.err.println("Unsupported operator: " + op);
                System.err.println("------------------");
                throw new RuntimeException("Unsupported operator: " + op);
        }
    }

    private boolean isTrue(Object val) {
        if (val instanceof Boolean)
            return (Boolean) val;
        if (val instanceof Number)
            return ((Number) val).doubleValue() != 0;
        if (val instanceof String)
            return ((String) val).length() > 0;
        return val != null;
    }

    private boolean equalsValue(double l, double r) {
        return l == r;
    }

    private double toNumber(Object val) {
        if (val instanceof Number)
            return ((Number) val).doubleValue();
        if (val instanceof String) {
            try {
                return Double.parseDouble((String) val);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        if (val instanceof Boolean)
            return (Boolean) val ? 1 : 0;
        return 0;
    }

    private int toInteger(Object val) {
        if (val instanceof Integer)
            return (Integer) val;
        if (val instanceof Double)
            return (int) ((Double) val).doubleValue();
        if (val instanceof String) {
            try {
                return Integer.parseInt((String) val);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        if (val instanceof Boolean)
            return (Boolean) val ? 1 : 0;
        return 0;
    }

    private boolean isInteger(double d) {
        return d == Math.floor(d);
    }

    private Optional<Object> getVariable(String name) {
        for (Map<String, Object> scope : callStack) {
            if (scope.containsKey(name)) {
                return Optional.of(scope.get(name));
            }
        }
        return Optional.empty();
    }

    private void setVariable(String name, Object value) {
        for (Map<String, Object> scope : callStack) {
            if (scope.containsKey(name)) {
                scope.put(name, value);
                return;
            }
        }
        // If variable not found in any scope, set in current (top) scope
        callStack.peek().put(name, value);
    }

    // Function to call a function externally (optional)
    public Object callFunction(String name, List<Object> args) {
        FunctionDefNode func = functions.get(name);
        if (func == null) {
            throw new RuntimeException("Undefined function: " + name);
        }
        if (args.size() != func.parameters.size()) {
            throw new RuntimeException("Function " + name + " expects " + func.parameters.size() + " arguments but got " + args.size());
        }

        // Create a new scope for function
        Map<String, Object> localScope = new HashMap<>();
        for (int i = 0; i < func.parameters.size(); i++) {
            localScope.put(func.parameters.get(i), args.get(i));
        }
        callStack.push(localScope);

        try {
            executeBlock(func.body);
        } catch (ReturnException re) {
            callStack.pop();
            return re.value;
        }

        callStack.pop();
        return null;
    }
}
