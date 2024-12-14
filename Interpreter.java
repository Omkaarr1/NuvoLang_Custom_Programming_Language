import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

class ReturnException extends RuntimeException {
    public final Object value;

    public ReturnException(Object value) {
        this.value = value;
    }
}

class Variable {
    Object value;
    boolean isEncrypted;

    Variable(Object value, boolean isEncrypted) {
        this.value = value;
        this.isEncrypted = isEncrypted;
    }
}

public class Interpreter {
    private final Map<String, FunctionDefNode> functions = new HashMap<>();
    private final Deque<Map<String, Variable>> callStack = new ArrayDeque<>();
    private final Scanner scanner = new Scanner(System.in);

    private static final String ENCRYPTION_KEY = "0123456789abcdef";
    private static final String INIT_VECTOR = "abcdef9876543210";

    public Interpreter() {
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
            Object val = evaluateForPrint(((PrintNode) node).expr);
            System.out.println(val);
        } else if (node instanceof IfNode) {
            IfNode ifNode = (IfNode) node;
            Object condition = evaluate(ifNode.condition);
            if (isTrue(condition)) {
                executeBlock(ifNode.ifBranch);
            } else if (ifNode.elseBranch != null) {
                executeBlock(ifNode.elseBranch);
            }
        } else if (node instanceof ForNode) {
            ForNode forNode = (ForNode) node;
            evaluate(forNode.initialization);
            while (isTrue(evaluate(forNode.condition))) {
                executeBlock(forNode.body);
                evaluate(forNode.increment);
            }
        } else if (node instanceof WhileNode) {
            WhileNode whileNode = (WhileNode) node;
            while (isTrue(evaluate(whileNode.condition))) {
                executeBlock(whileNode.body);
            }
        } else if (node instanceof InputNode) {
            InputNode inputNode = (InputNode) node;
            Object promptObj = evaluate(((InputNode) node).prompt);
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
            boolean isEncrypted = false;
            String actualVarName = varName;
            if (varName.startsWith("@ENC")) {
                isEncrypted = true;
                actualVarName = varName.substring(4);
            }
            Object value = parseInputValue(userInput);
            if (isEncrypted) {
                String encryptedValue = encrypt(String.valueOf(value));
                setVariable(actualVarName, encryptedValue, true);
            } else {
                setVariable(actualVarName, value, false);
            }
        } else if (node instanceof ExpressionStatement) {
            evaluate(((ExpressionStatement) node).expr);
        } else if (node instanceof FunctionDefNode) {
            FunctionDefNode func = (FunctionDefNode) node;
            functions.put(func.name, func);
        } else if (node instanceof ReturnNode) {
            ReturnNode ret = (ReturnNode) node;
            Object value = ret.value != null ? evaluate(ret.value) : null;
            throw new ReturnException(value);
        } else if (node instanceof EventTriggerNode) {
            EventTriggerNode etn = (EventTriggerNode) node;
            Object timeVal = evaluate(etn.timeExpr);
            // Currently hard-coded to null:
            // scheduleEvent(timeVal, etn.unit, etn.action, null);

            // UPDATED CODE:
            Object timesVal = null;
            if (etn.timesExpr != null) {
                timesVal = evaluate(etn.timesExpr);
            }

            scheduleEvent(timeVal, etn.unit, etn.action, timesVal);
        } else {
            throw new RuntimeException("Unknown node type: " + node.getClass().getName());
        }
    }

    private void executeBlock(List<Node> statements) {
        for (Node stmt : statements) {
            executeNode(stmt);
        }
    }

    private Object parseInputValue(String userInput) {
        if (userInput.equalsIgnoreCase("true") || userInput.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(userInput);
        }
        try {
            if (userInput.contains(".")) {
                return Double.parseDouble(userInput);
            } else {
                return Integer.parseInt(userInput);
            }
        } catch (NumberFormatException e) {
            userInput = userInput.trim();
            if (userInput.startsWith("[") && userInput.endsWith("]")) {
                String elementsStr = userInput.substring(1, userInput.length() - 1).trim();
                if (elementsStr.isEmpty()) {
                    return new ArrayList<Object>();
                } else {
                    String[] elements = elementsStr.split(",");
                    List<Object> list = new ArrayList<>();
                    for (String elem : elements) {
                        elem = elem.trim();
                        if (elem.equalsIgnoreCase("true") || elem.equalsIgnoreCase("false")) {
                            list.add(Boolean.parseBoolean(elem));
                        } else {
                            try {
                                if (elem.contains(".")) {
                                    list.add(Double.parseDouble(elem));
                                } else {
                                    list.add(Integer.parseInt(elem));
                                }
                            } catch (NumberFormatException ex) {
                                list.add(elem.replaceAll("^\"|\"$", ""));
                            }
                        }
                    }
                    return list;
                }
            } else {
                if (userInput.startsWith("\"") && userInput.endsWith("\"")) {
                    return userInput.substring(1, userInput.length() - 1);
                } else {
                    return userInput;
                }
            }
        }
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
        return evaluate(node, true);
    }

    private Object evaluate(Node node, boolean decrypt) {
        if (node instanceof LiteralNode) {
            return ((LiteralNode) node).value;
        } else if (node instanceof ArrayLiteralNode) {
            List<Object> list = new ArrayList<>();
            for (Node elem : ((ArrayLiteralNode) node).elements) {
                list.add(evaluate(elem, decrypt));
            }
            return list;
        } else if (node instanceof VariableNode) {
            String varName = ((VariableNode) node).name;
            boolean isEncrypted = false;
            String actualVarName = varName;
            if (varName.startsWith("@ENC")) {
                isEncrypted = true;
                actualVarName = varName.substring(4);
            }
            Optional<Variable> varOpt = getVariable(actualVarName);
            if (!varOpt.isPresent()) {
                throw new RuntimeException("Undefined variable: " + varName);
            }
            Variable var = varOpt.get();
            if (var.isEncrypted && decrypt) {
                String decryptedValue = decrypt(var.value.toString());
                return parseValue(decryptedValue);
            } else {
                return var.value;
            }
        } else if (node instanceof AssignNode) {
            AssignNode assign = (AssignNode) node;
            String varName = assign.name;
            boolean isEncrypted = false;
            String actualVarName = varName;
            if (varName.startsWith("@ENC")) {
                isEncrypted = true;
                actualVarName = varName.substring(4);
            }
            Object rightVal = evaluate(assign.value, decrypt);
            if (assign.op == TokenType.ASSIGN) {
                if (isEncrypted) {
                    String encryptedValue = encrypt(String.valueOf(rightVal));
                    setVariable(actualVarName, encryptedValue, true);
                    return encryptedValue;
                } else {
                    setVariable(actualVarName, rightVal, false);
                    return rightVal;
                }
            } else {
                Optional<Variable> varOpt = getVariable(actualVarName);
                if (!varOpt.isPresent()) {
                    throw new RuntimeException("Undefined variable: " + varName);
                }
                Variable var = varOpt.get();
                Object leftVal;
                if (var.isEncrypted) {
                    leftVal = parseValue(decrypt(var.value.toString()));
                } else {
                    leftVal = var.value;
                }
                Object newVal = applyOp(leftVal, rightVal, operatorFromCompound(assign.op));
                if (isEncrypted) {
                    String encryptedNewVal = encrypt(String.valueOf(newVal));
                    setVariable(actualVarName, encryptedNewVal, true);
                    return encryptedNewVal;
                } else {
                    setVariable(actualVarName, newVal, false);
                    return newVal;
                }
            }
        } else if (node instanceof BinaryNode) {
            Object left = evaluate(((BinaryNode) node).left, decrypt);
            Object right = evaluate(((BinaryNode) node).right, decrypt);
            return applyOp(left, right, ((BinaryNode) node).op);
        } else if (node instanceof UnaryNode) {
            UnaryNode un = (UnaryNode) node;
            if (un.postfix) {
                if (!(un.expr instanceof VariableNode)) {
                    throw new RuntimeException("Postfix operator requires a variable.");
                }
                String varName = ((VariableNode) un.expr).name;
                boolean isEncrypted = false;
                String actualVarName = varName;
                if (varName.startsWith("@ENC")) {
                    isEncrypted = true;
                    actualVarName = varName.substring(4);
                }
                Optional<Variable> varOpt = getVariable(actualVarName);
                if (!varOpt.isPresent()) {
                    throw new RuntimeException("Undefined variable: " + varName);
                }
                Variable var = varOpt.get();
                Object val;
                if (var.isEncrypted) {
                    val = parseValue(decrypt(var.value.toString()));
                } else {
                    val = var.value;
                }
                Object retVal = val;
                Object newVal = applyUnary(val, un.op);
                if (isEncrypted) {
                    String encryptedNewVal = encrypt(String.valueOf(newVal));
                    setVariable(actualVarName, encryptedNewVal, true);
                } else {
                    setVariable(actualVarName, newVal, false);
                }
                return retVal;
            } else {
                if (un.op == TokenType.PLUS_PLUS || un.op == TokenType.MINUS_MINUS) {
                    if (!(un.expr instanceof VariableNode)) {
                        throw new RuntimeException("Prefix operator requires a variable.");
                    }
                    String varName = ((VariableNode) un.expr).name;
                    boolean isEncrypted = false;
                    String actualVarName = varName;
                    if (varName.startsWith("@ENC")) {
                        isEncrypted = true;
                        actualVarName = varName.substring(4);
                    }
                    Optional<Variable> varOpt = getVariable(actualVarName);
                    if (!varOpt.isPresent()) {
                        throw new RuntimeException("Undefined variable: " + varName);
                    }
                    Variable var = varOpt.get();
                    Object val;
                    if (var.isEncrypted) {
                        val = parseValue(decrypt(var.value.toString()));
                    } else {
                        val = var.value;
                    }
                    Object newVal = applyUnary(val, un.op);
                    if (isEncrypted) {
                        String encryptedNewVal = encrypt(String.valueOf(newVal));
                        setVariable(actualVarName, encryptedNewVal, true);
                        return parseValue(decrypt(encryptedNewVal));
                    } else {
                        setVariable(actualVarName, newVal, false);
                        return newVal;
                    }
                } else if (un.op == TokenType.NOT) {
                    Object val = evaluate(un.expr, decrypt);
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
                throw new RuntimeException("Function " + call.name + " expects " + func.parameters.size()
                        + " arguments but got " + call.arguments.size());
            }
            List<Object> argValues = new ArrayList<>();
            for (Node arg : call.arguments) {
                argValues.add(evaluate(arg, decrypt));
            }
            Map<String, Variable> localScope = new HashMap<>();
            for (int i = 0; i < func.parameters.size(); i++) {
                localScope.put(func.parameters.get(i), new Variable(argValues.get(i), false));
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
        } else if (node instanceof ReturnNode) {
            ReturnNode ret = (ReturnNode) node;
            Object value = ret.value != null ? evaluate(ret.value, decrypt) : null;
            throw new ReturnException(value);
        }

        throw new RuntimeException("Unknown node type: " + node.getClass().getName());
    }

    private Object evaluateForPrint(Node node) {
        return evaluate(node, false);
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

        if (left instanceof List && op == TokenType.PLUS) {
            if (right instanceof List) {
                List<Object> combined = new ArrayList<>((List<Object>) left);
                combined.addAll((List<Object>) right);
                return combined;
            }
        }

        if (left instanceof List && (op == TokenType.EQ_EQ || op == TokenType.NOT_EQ)) {
            boolean equals = left.equals(right);
            return op == TokenType.EQ_EQ ? equals : !equals;
        }

        double l = toNumber(left);
        double r = toNumber(right);

        switch (op) {
            case PLUS:
                if (isInteger(l) && isInteger(r))
                    return (int) (l + r);
                return (l + r);
            case MINUS:
                if (isInteger(l) && isInteger(r))
                    return (int) (l - r);
                return (l - r);
            case STAR:
                if (isInteger(l) && isInteger(r))
                    return (int) (l * r);
                return (l * r);
            case SLASH:
                if (r == 0) {
                    throw new RuntimeException("Division by zero");
                }
                if (isInteger(l) && isInteger(r))
                    return (int) (l / r);
                return (l / r);
            case MOD:
                if (r == 0) {
                    throw new RuntimeException("Division by zero");
                }
                return (int) l % (int) r;
            case EQ_EQ:
                return l == r;
            case NOT_EQ:
                return l != r;
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
        if (val instanceof List)
            return ((List<?>) val).size() > 0;
        return val != null;
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

    private boolean isInteger(double d) {
        return d == Math.floor(d);
    }

    private Optional<Variable> getVariable(String name) {
        for (Map<String, Variable> scope : callStack) {
            if (scope.containsKey(name)) {
                return Optional.of(scope.get(name));
            }
        }
        return Optional.empty();
    }

    private void setVariable(String name, Object value, boolean isEncrypted) {
        for (Map<String, Variable> scope : callStack) {
            if (scope.containsKey(name)) {
                scope.put(name, new Variable(value, isEncrypted));
                return;
            }
        }
        callStack.peek().put(name, new Variable(value, isEncrypted));
    }

    public Object callFunction(String name, List<Object> args) {
        FunctionDefNode func = functions.get(name);
        if (func == null) {
            throw new RuntimeException("Undefined function: " + name);
        }
        if (args.size() != func.parameters.size()) {
            throw new RuntimeException(
                    "Function " + name + " expects " + func.parameters.size() + " arguments but got " + args.size());
        }

        Map<String, Variable> localScope = new HashMap<>();
        for (int i = 0; i < func.parameters.size(); i++) {
            localScope.put(func.parameters.get(i), new Variable(args.get(i), false));
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

    private String encrypt(String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.getBytes("UTF-8"), "AES");
            IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR.getBytes("UTF-8"));
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);
            byte[] encrypted = cipher.doFinal(value.getBytes("UTF-8"));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception ex) {
            throw new RuntimeException("Encryption failed: " + ex.getMessage());
        }
    }

    private String decrypt(String encrypted) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            SecretKeySpec key = new SecretKeySpec(ENCRYPTION_KEY.getBytes("UTF-8"), "AES");
            IvParameterSpec iv = new IvParameterSpec(INIT_VECTOR.getBytes("UTF-8"));
            cipher.init(Cipher.DECRYPT_MODE, key, iv);
            byte[] decoded = Base64.getDecoder().decode(encrypted);
            byte[] original = cipher.doFinal(decoded);
            return new String(original, "UTF-8");
        } catch (Exception ex) {
            throw new RuntimeException("Decryption failed: " + ex.getMessage());
        }
    }

    private Object parseValue(String value) {
        if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")) {
            return Boolean.parseBoolean(value);
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            // not int
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            // not double
        }
        value = value.trim();
        if (value.startsWith("[") && value.endsWith("]")) {
            String elementsStr = value.substring(1, value.length() - 1).trim();
            if (elementsStr.isEmpty()) {
                return new ArrayList<Object>();
            } else {
                String[] elements = elementsStr.split(",");
                List<Object> list = new ArrayList<>();
                for (String elem : elements) {
                    elem = elem.trim();
                    if (elem.equalsIgnoreCase("true") || elem.equalsIgnoreCase("false")) {
                        list.add(Boolean.parseBoolean(elem));
                    } else {
                        try {
                            if (elem.contains(".")) {
                                list.add(Double.parseDouble(elem));
                            } else {
                                list.add(Integer.parseInt(elem));
                            }
                        } catch (NumberFormatException ex) {
                            list.add(elem.replaceAll("^\"|\"$", ""));
                        }
                    }
                }
                return list;
            }
        }
        return value;
    }

    private void scheduleEvent(Object timeVal, String unit, Node action, Object timesVal) {
        int times = -1; // -1 means unlimited runs
        if (timesVal != null) {
            times = (int) toNumber(timesVal);
            if (times < 1) {
                throw new RuntimeException("The number of times to run must be a positive integer.");
            }
        }

        // If this is a time-based (seconds, minutes, hours) trigger
        if (unit != null) {
            double val = toNumber(timeVal);
            long delayMillis;
            switch (unit.toLowerCase()) {
                case "seconds":
                    delayMillis = (long) (val * 1000);
                    break;
                case "minutes":
                    delayMillis = (long) (val * 60 * 1000);
                    break;
                case "hours":
                    delayMillis = (long) (val * 3600 * 1000);
                    break;
                default:
                    throw new RuntimeException("Unknown time unit: " + unit);
            }

            // If times is -1, run indefinitely using a repeating schedule
            if (times == -1) {
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        executeNode(action);
                    }
                }, delayMillis, delayMillis);
            } else {
                // If we have a limited number of runs, use a recursive scheduling approach
                runLimitedTimes(action, delayMillis, times);
            }

        } else {
            // This is a datetime-based trigger
            if (!(timeVal instanceof String)) {
                throw new RuntimeException("DateTime trigger must be a string in the format 'YYYY-MM-DD HH:mm:ss'");
            }
            String dateTimeStr = (String) timeVal;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime targetTime;
            try {
                targetTime = LocalDateTime.parse(dateTimeStr, formatter);
            } catch (Exception e) {
                throw new RuntimeException("Invalid datetime format. Use 'YYYY-MM-DD HH:mm:ss'");
            }

            long targetMillis = targetTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long delay = targetMillis - System.currentTimeMillis();
            if (delay < 0) {
                throw new RuntimeException("Target time is in the past.");
            }

            // For datetime triggers, we only run once. If timesVal was provided, it's not
            // supported.
            if (timesVal != null) {
                throw new RuntimeException("Times parameter not supported for datetime triggers.");
            }

            // Schedule a single execution at the given future time
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    executeNode(action);
                }
            }, delay);
        }
    }

    /**
     * Recursively schedules a single-run task until the specified number of times
     * is reached.
     * 
     * @param action        The node action to execute each run.
     * @param delayMillis   The delay/interval in milliseconds between runs.
     * @param remainingRuns How many more times to run.
     */
    private void runLimitedTimes(Node action, long delayMillis, int remainingRuns) {
        if (remainingRuns <= 0) return; // No runs left
    
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                // Execute the action this run
                executeNode(action);
    
                // Decrement the run count
                int nextRunCount = remainingRuns - 1;
    
                if (nextRunCount > 0) {
                    // Still have runs left, schedule again
                    runLimitedTimes(action, delayMillis, nextRunCount);
                } else {
                    // No runs left, exit the program
                    System.exit(0);
                }
            }
        }, delayMillis);
    }
    
}
