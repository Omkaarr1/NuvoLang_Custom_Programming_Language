import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;

import weka.core.Instances;
import weka.core.Attribute;
import weka.core.converters.CSVLoader;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.functions.LinearRegression;
import weka.clusterers.SimpleKMeans;
import weka.classifiers.Evaluation;
import weka.clusterers.ClusterEvaluation;
import weka.core.SerializationHelper;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.NumericToNominal;
import weka.filters.unsupervised.attribute.StringToNominal;

import java.io.File;
import java.io.IOException;
import java.util.Random;

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
            Object timesVal = null;
            if (etn.timesExpr != null) {
                timesVal = evaluate(etn.timesExpr);
            }
            scheduleEvent(timeVal, etn.unit, etn.action, timesVal);
        } else if (node instanceof UseNode) {
            UseNode useNode = (UseNode) node;
            loadLibrary(useNode.libraryName);
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
        } else if (node instanceof ObjectMethodCallNode) {
            ObjectMethodCallNode om = (ObjectMethodCallNode) node;
            Object targetVal = evaluate(om.target, decrypt);
            List<Object> argVals = new ArrayList<>();
            for (Node arg : om.arguments) {
                argVals.add(evaluate(arg, decrypt));
            }
            return callObjectMethod(targetVal, om.methodName, argVals);
        }

        throw new RuntimeException("Unknown node type: " + node.getClass().getName());
    }

    private void loadLibrary(String name) {
        if (name.equals("ml")) {
            setVariable("ml", new MlLibrary(), false);
        } else {
            throw new RuntimeException("Unknown library: " + name);
        }
    }

    private Object callObjectMethod(Object target, String methodName, List<Object> args) {
        if (target instanceof MlLibrary) {
            MlLibrary ml = (MlLibrary) target;
            switch (methodName) {
                case "randomforest":
                    if (args.size() == 1 && args.get(0) instanceof String) {
                        return ml.randomforest((String) args.get(0));
                    } else if (args.size() == 2 && args.get(0) instanceof String && args.get(1) instanceof String) {
                        return ml.randomforest((String) args.get(0), (String) args.get(1));
                    } else {
                        throw new RuntimeException("Invalid arguments for ml.randomforest");
                    }
                case "linearregression":
                    if (args.size() == 1 && args.get(0) instanceof String) {
                        return ml.linearregression((String) args.get(0));
                    } else if (args.size() == 2 && args.get(0) instanceof String && args.get(1) instanceof String) {
                        return ml.linearregression((String) args.get(0), (String) args.get(1));
                    } else {
                        throw new RuntimeException("Invalid arguments for ml.linearregression");
                    }
                case "kmeans":
                    if (args.size() == 1 && args.get(0) instanceof String) {
                        return ml.kmeans((String) args.get(0));
                    } else {
                        throw new RuntimeException("Invalid arguments for ml.kmeans");
                    }
                // Add more methods as needed
            }
        }
        throw new RuntimeException("Unknown method " + methodName + " on object " + target);
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

            if (times == -1) {
                Timer timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        executeNode(action);
                    }
                }, delayMillis, delayMillis);
            } else {
                runLimitedTimes(action, delayMillis, times);
            }

        } else {
            if (!(timeVal instanceof String)) {
                throw new RuntimeException("DateTime trigger must be a string in 'YYYY-MM-DD HH:mm:ss' format");
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

            if (timesVal != null) {
                throw new RuntimeException("Times parameter not supported for datetime triggers.");
            }

            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    executeNode(action);
                }
            }, delay);
        }
    }

    private void runLimitedTimes(Node action, long delayMillis, int remainingRuns) {
        if (remainingRuns <= 0) return;

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                executeNode(action);
                int nextRunCount = remainingRuns - 1;
                if (nextRunCount > 0) {
                    runLimitedTimes(action, delayMillis, nextRunCount);
                } else {
                    System.exit(0);
                }
            }
        }, delayMillis);
    }
}

class MlLibrary {
    private Instances loadData(String csv, String targetColumn) throws IOException, Exception {
        CSVLoader loader = new CSVLoader();
        loader.setSource(new File(csv));
        Instances data = loader.getDataSet();

        if (targetColumn != null && !targetColumn.isEmpty()) {
            int targetIndex = -1;
            for (int i = 0; i < data.numAttributes(); i++) {
                Attribute attr = data.attribute(i);
                if (attr.name().equalsIgnoreCase(targetColumn)) {
                    targetIndex = i;
                    break;
                }
            }
            if (targetIndex == -1) {
                throw new RuntimeException("Column '" + targetColumn + "' not found in the dataset.");
            }
            data.setClassIndex(targetIndex);
        } else {
            data.setClassIndex(data.numAttributes() - 1);
        }

        // Convert any string attributes to nominal
        StringToNominal stn = new StringToNominal();
        stn.setAttributeRange("first-last");
        stn.setInputFormat(data);
        data = Filter.useFilter(data, stn);

        return data;
    }

    public Object randomforest(String csv) {
        return randomforest(csv, null);
    }

    public Object randomforest(String csv, String column) {
        try {
            Instances data = loadData(csv, column);

            // If class attribute is not nominal, try converting it.
            if (!data.classAttribute().isNominal()) {
                System.out.println("[ml] Class attribute is not nominal. Attempting NumericToNominal...");
                NumericToNominal convert = new NumericToNominal();
                convert.setAttributeIndices(String.valueOf(data.classIndex() + 1));
                convert.setInputFormat(data);
                data = Filter.useFilter(data, convert);

                if (!data.classAttribute().isNominal()) {
                    System.out.println("[ml] Failed to convert class attribute to nominal. Metrics won't be printed.");
                } else {
                    System.out.println("[ml] Successfully converted class attribute to nominal.");
                }
            }

            RandomForest rf = new RandomForest();
            rf.buildClassifier(data);
            System.out.println("[ml] Random Forest trained on " + csv +
                (column != null ? " with target column '" + column + "'" : ""));
            System.out.println("[ml] Model Summary:\n" + rf.toString());

            Evaluation eval = new Evaluation(data);
            eval.crossValidateModel(rf, data, 10, new Random(1));

            if (data.classAttribute().isNominal()) {
                double accuracy = eval.pctCorrect();
                System.out.println("[ml] Accuracy: " + accuracy + "%");

                for (int i = 0; i < data.numClasses(); i++) {
                    double precision = eval.precision(i);
                    double recall = eval.recall(i);
                    double f1 = eval.fMeasure(i);
                    String className = data.classAttribute().value(i);
                    System.out.println("[ml] Class: " + className);
                    System.out.println("    Precision: " + precision);
                    System.out.println("    Recall: " + recall);
                    System.out.println("    F1-Score: " + f1);
                }
            } else {
                System.out.println("[ml] Class is not nominal, no accuracy/precision/F1 printed.");
            }

            SerializationHelper.write("randomforest.model", rf);
            System.out.println("[ml] Random Forest model saved to 'randomforest.model'");

            return rf;
        } catch (Exception e) {
            throw new RuntimeException("Error training Random Forest: " + e.getMessage(), e);
        }
    }

    public Object linearregression(String csv) {
        return linearregression(csv, null);
    }

    public Object linearregression(String csv, String column) {
        try {
            Instances data = loadData(csv, column);

            if (!data.classAttribute().isNumeric()) {
                System.out.println("[ml] Warning: The class attribute is not numeric. Linear Regression is intended for numeric targets.");
            }

            LinearRegression lr = new LinearRegression();
            lr.buildClassifier(data);
            System.out.println("[ml] Linear Regression trained on " + csv +
                (column != null ? " with target column '" + column + "'" : ""));
            System.out.println("[ml] Model Coefficients:\n" + lr);

            Evaluation eval = new Evaluation(data);
            eval.crossValidateModel(lr, data, 10, new Random(1));

            double corrCoef = eval.correlationCoefficient();
            double mae = eval.meanAbsoluteError();
            double rmse = eval.rootMeanSquaredError();

            System.out.println("[ml] Correlation Coefficient: " + corrCoef);
            System.out.println("[ml] Mean Absolute Error: " + mae);
            System.out.println("[ml] Root Mean Squared Error: " + rmse);

            SerializationHelper.write("linearregression.model", lr);
            System.out.println("[ml] Linear Regression model saved to 'linearregression.model'");

            return lr;
        } catch (Exception e) {
            throw new RuntimeException("Error training Linear Regression: " + e.getMessage(), e);
        }
    }

    public Object kmeans(String csv) {
        try {
            CSVLoader loader = new CSVLoader();
            loader.setSource(new File(csv));
            Instances data = loader.getDataSet();

            // Convert any string attributes to nominal for k-means
            StringToNominal stn = new StringToNominal();
            stn.setAttributeRange("first-last");
            stn.setInputFormat(data);
            data = Filter.useFilter(data, stn);

            SimpleKMeans kmeans = new SimpleKMeans();
            kmeans.setNumClusters(3);
            kmeans.buildClusterer(data);
            System.out.println("[ml] K-Means clustering on " + csv + " completed.");
            System.out.println("[ml] Cluster centroids: \n" + kmeans.toString());

            ClusterEvaluation clusterEval = new ClusterEvaluation();
            clusterEval.setClusterer(kmeans);
            clusterEval.evaluateClusterer(data);

            System.out.println("[ml] Number of clusters: " + kmeans.getNumClusters());

            int[] assignments = kmeans.getAssignments();
            int[] clusterCounts = new int[kmeans.getNumClusters()];
            for (int i = 0; i < assignments.length; i++) {
                clusterCounts[assignments[i]]++;
            }
            for (int i = 0; i < clusterCounts.length; i++) {
                System.out.println("[ml] Cluster " + i + ": " + clusterCounts[i] + " instances");
            }

            System.out.println("[ml] Note: Accuracy, Precision, and F1-Score are not applicable for clustering.");

            SerializationHelper.write("kmeans.model", kmeans);
            System.out.println("[ml] K-Means model saved to 'kmeans.model'");

            return kmeans;
        } catch (Exception e) {
            throw new RuntimeException("Error performing K-Means: " + e.getMessage(), e);
        }
    }
}
