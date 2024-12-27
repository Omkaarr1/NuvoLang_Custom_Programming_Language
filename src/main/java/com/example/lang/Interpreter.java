package com.example.lang;

import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.sql.*;

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

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.data.statistics.HistogramDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.File;
import java.io.IOException;

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
    
    @SuppressWarnings("unchecked")
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
        } else if (node instanceof AssignIndexNode) {
            AssignIndexNode assignIndex = (AssignIndexNode) node;
            Object target = evaluate(assignIndex.target, decrypt);
            Object indexObj = evaluate(assignIndex.index, decrypt);

            // Ensure the target is a list
            if (!(target instanceof List<?>)) {
                throw new RuntimeException("Target of indexing is not a list.");
            }
            List<Object> list = (List<Object>) target;

            // Convert index to integer
            int index = toInteger(indexObj);
            if (index < 0 || index >= list.size()) {
                throw new RuntimeException("Index out of bounds: " + index);
            }

            // Evaluate the value to assign
            Object value = evaluate(assignIndex.value, decrypt);

            // Handle compound assignment operators
            if (assignIndex.op != TokenType.ASSIGN) {
                Object currentVal = list.get(index);
                Object newVal = applyOp(currentVal, value, operatorFromCompound(assignIndex.op));
                list.set(index, newVal);
                return newVal;
            } else {
                list.set(index, value);
                return value;
            }
        } else if (node instanceof IndexNode) {
            IndexNode indexNode = (IndexNode) node;
            Object target = evaluate(indexNode.target, decrypt);
            Object indexObj = evaluate(indexNode.index, decrypt);

            // Ensure the target is a list
            if (!(target instanceof List<?>)) {
                throw new RuntimeException("Target of indexing is not a list.");
            }
            List<Object> list = (List<Object>) target; // Unchecked cast

            // Convert index to integer
            int index = toInteger(indexObj);
            if (index < 0 || index >= list.size()) {
                throw new RuntimeException("Index out of bounds: " + index);
            }

            return list.get(index);
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

    /**
     * Converts an Object to an integer. Supports Integer, Double, and String
     * representations.
     *
     * @param obj The object to convert.
     * @return The integer value.
     */
    private int toInteger(Object obj) {
        if (obj instanceof Integer) {
            return (Integer) obj;
        } else if (obj instanceof Double) {
            return ((Double) obj).intValue();
        } else if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Cannot convert to integer: " + obj);
            }
        } else {
            throw new RuntimeException("Cannot convert to integer: " + obj);
        }
    }

    private void loadLibrary(String name) {
        if (name.equals("ml")) {
            setVariable("ml", new MlLibrary(), false);
        } else if (name.equals("blockchain")) {
            setVariable("blockchain", new BlockchainLibrary(), false);
        } else if (name.equals("data_science") || name.equals("data science")) { // Handle different naming conventions
            setVariable("data_science", new DataScienceLibrary(), false);
        } else if (name.equals("database")) {
            setVariable("db", new DatabaseLibrary(), false);
        } else {
            throw new RuntimeException("Unknown library: " + name);
        }
    }

    // Interpreter.java

    /**
     * Calls a method on an object from a loaded library or other supported objects.
     *
     * @param target     The target object.
     * @param methodName The method name to call.
     * @param args       The arguments to pass to the method.
     * @return The result of the method call.
     */
    @SuppressWarnings("unchecked")
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
                default:
                    throw new RuntimeException("Unknown method " + methodName + " on ml object");
            }
        } else if (target instanceof BlockchainLibrary) {
            BlockchainLibrary bc = (BlockchainLibrary) target;
            switch (methodName) {
                case "init":
                    if (args.size() == 2 && args.get(0) instanceof String && args.get(1) instanceof Number) {
                        return bc.init((String) args.get(0), ((Number) args.get(1)).doubleValue());
                    } else {
                        throw new RuntimeException("Invalid arguments for blockchain.init");
                    }
                case "transaction":
                    if (args.size() == 2 && args.get(0) instanceof String && args.get(1) instanceof Number) {
                        return bc.transaction((String) args.get(0), ((Number) args.get(1)).doubleValue());
                    } else {
                        throw new RuntimeException("Invalid arguments for blockchain.transaction");
                    }
                case "showCurrentBalance":
                    if (args.size() == 0) {
                        return bc.showCurrentBalance();
                    } else {
                        throw new RuntimeException("showCurrentBalance does not take any arguments.");
                    }
                case "showTransactionHistory":
                    if (args.size() == 0) {
                        return bc.showTransactionHistory();
                    } else {
                        throw new RuntimeException("showTransactionHistory does not take any arguments.");
                    }
                default:
                    throw new RuntimeException("Unknown method " + methodName + " on blockchain object");
            }
        } else if (target instanceof DataScienceLibrary) {
            DataScienceLibrary ds = (DataScienceLibrary) target;
            switch (methodName) {
                case "loadCSV":
                    if (args.size() == 1 && args.get(0) instanceof String) {
                        return ds.loadCSV((String) args.get(0));
                    } else {
                        throw new RuntimeException("Invalid arguments for data_science.loadCSV");
                    }
                case "calculateMean":
                    if (args.size() == 2 && args.get(0) instanceof Instances && args.get(1) instanceof String) {
                        return ds.calculateMean((Instances) args.get(0), (String) args.get(1));
                    } else {
                        throw new RuntimeException("Invalid arguments for data_science.calculateMean");
                    }
                case "calculateMedian":
                    if (args.size() == 2 && args.get(0) instanceof Instances && args.get(1) instanceof String) {
                        return ds.calculateMedian((Instances) args.get(0), (String) args.get(1));
                    } else {
                        throw new RuntimeException("Invalid arguments for data_science.calculateMedian");
                    }
                case "calculateStdDev":
                    if (args.size() == 2 && args.get(0) instanceof Instances && args.get(1) instanceof String) {
                        return ds.calculateStdDev((Instances) args.get(0), (String) args.get(1));
                    } else {
                        throw new RuntimeException("Invalid arguments for data_science.calculateStdDev");
                    }
                case "plotHistogram":
                    if (args.size() == 3 && args.get(0) instanceof Instances && args.get(1) instanceof String
                            && args.get(2) instanceof String) {
                        ds.plotHistogram((Instances) args.get(0), (String) args.get(1), (String) args.get(2));
                        return null; // plotHistogram is void
                    } else {
                        throw new RuntimeException("Invalid arguments for data_science.plotHistogram");
                    }
                case "plotScatter":
                    if (args.size() == 4 && args.get(0) instanceof Instances && args.get(1) instanceof String
                            && args.get(2) instanceof String && args.get(3) instanceof String) {
                        ds.plotScatter((Instances) args.get(0), (String) args.get(1), (String) args.get(2),
                                (String) args.get(3));
                        return null; // plotScatter is void
                    } else {
                        throw new RuntimeException("Invalid arguments for data_science.plotScatter");
                    }
                case "filterData":
                    if (args.size() == 4 && args.get(0) instanceof Instances && args.get(1) instanceof String
                            && args.get(2) instanceof String && args.get(3) instanceof Number) {
                        return ds.filterData((Instances) args.get(0), (String) args.get(1), (String) args.get(2),
                                ((Number) args.get(3)).doubleValue());
                    } else {
                        throw new RuntimeException("Invalid arguments for data_science.filterData");
                    }
                    // Add more cases for additional DataScienceLibrary methods as needed
                default:
                    throw new RuntimeException("Unknown method " + methodName + " on data_science object");
            }
        } else if (target instanceof Instances) {
            Instances instances = (Instances) target;
            switch (methodName) {
                case "numInstances":
                    if (args.size() == 0) {
                        return instances.numInstances();
                    } else {
                        throw new RuntimeException("numInstances method does not take any arguments.");
                    }
                    // Add more Instances methods as needed
                default:
                    throw new RuntimeException("Unknown method " + methodName + " on Instances object.");
            }
        } else if (target instanceof List) { // Updated condition to use raw List
            List<?> list = (List<?>) target;
            switch (methodName) {
                case "numInstances":
                    if (args.size() == 0) {
                        return list.size();
                    } else {
                        throw new RuntimeException("numInstances method does not take any arguments.");
                    }
                case "add":
                    if (args.size() == 1) {
                        ((List<Object>) list).add(args.get(0));
                        return null;
                    } else {
                        throw new RuntimeException("add method expects exactly one argument.");
                    }
                case "remove":
                    if (args.size() == 1 && args.get(0) instanceof Integer) {
                        ((List<Object>) list).remove((Integer) args.get(0));
                        return null;
                    } else {
                        throw new RuntimeException("remove method expects exactly one integer argument.");
                    }
                    // Add more List methods as needed
                default:
                    throw new RuntimeException("Unknown method '" + methodName + "' on List object.");
            }
        } else if (target instanceof DatabaseLibrary) {
            DatabaseLibrary db = (DatabaseLibrary) target;
            switch (methodName) {
                case "connect":
                    if (args.size() == 3 && args.get(0) instanceof String && args.get(1) instanceof String
                            && args.get(2) instanceof String) {
                        return db.connect((String) args.get(0), (String) args.get(1), (String) args.get(2));
                    } else {
                        throw new RuntimeException(
                                "Invalid arguments for db.connect. Expected (String url, String user, String password).");
                    }
                case "query":
                    if (args.size() == 1 && args.get(0) instanceof String) {
                        return db.query((String) args.get(0));
                    } else {
                        throw new RuntimeException("Invalid arguments for db.query. Expected (String sql).");
                    }
                case "close":
                    if (args.size() == 0) {
                        return db.close();
                    } else {
                        throw new RuntimeException("close method does not take any arguments.");
                    }
                default:
                    throw new RuntimeException("Unknown method '" + methodName + "' on db object.");
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

    // Interpreter.java

    /**
     * Combines two lists into a new list containing elements from both.
     *
     * @param left  The first list to combine.
     * @param right The second list to combine.
     * @return A new list containing elements from both left and right lists.
     */
    public List<Object> combineLists(Object left, Object right) {
        if (left instanceof List<?> && right instanceof List<?>) {
            List<?> leftList = (List<?>) left;
            List<?> rightList = (List<?>) right;

            List<Object> combined = new ArrayList<>();
            combined.addAll(leftList);
            combined.addAll(rightList);
            return combined;
        } else {
            throw new RuntimeException("Both left and right must be instances of List<?>.");
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
        if (remainingRuns <= 0)
            return;

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
                System.out.println(
                        "[ml] Warning: The class attribute is not numeric. Linear Regression is intended for numeric targets.");
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

class BlockchainLibrary {
    private String privateKey;
    private double balance;
    private String fromAddress;
    private List<Transaction> transactionHistory; // List to store transaction history

    // Inner class to represent a transaction
    private class Transaction {
        String toAddress;
        double amount;
        String transactionID;
        int hashCode;

        Transaction(String toAddress, double amount) {
            this.toAddress = toAddress;
            this.amount = amount;
            this.transactionID = java.util.UUID.randomUUID().toString();
            this.hashCode = (fromAddress + toAddress + amount + transactionID).hashCode();
        }
    }

    public BlockchainLibrary() {
        this.transactionHistory = new ArrayList<>();
    }

    public Object init(String privateKey, double amount) {
        this.privateKey = privateKey;
        this.balance = amount;
        this.fromAddress = Integer.toHexString(privateKey.hashCode());
        System.out.println("[blockchain] Initialized:");
        System.out.println("    Address: " + fromAddress);
        System.out.println("    Balance: " + balance);
        return null;
    }

    public Object transaction(String toAddress, double amount) {
        if (amount > balance) {
            System.out.println("[blockchain] Transaction failed: insufficient funds.");
            return null;
        }
        balance -= amount;
        Transaction tx = new Transaction(toAddress, amount);
        transactionHistory.add(tx);

        System.out.println("[blockchain] Transaction successful!");
        System.out.println("    hashCode: " + tx.hashCode);
        System.out.println("    transactionID: " + tx.transactionID);
        System.out.println("    amount: " + tx.amount);
        System.out.println("    to Address: " + tx.toAddress);

        return null;
    }

    // New method to show current balance
    public Object showCurrentBalance() {
        System.out.println("[blockchain] Current Balance: " + balance);
        return balance;
    }

    // New method to show transaction history
    public Object showTransactionHistory() {
        if (transactionHistory.isEmpty()) {
            System.out.println("[blockchain] No transactions found.");
            return null;
        }

        System.out.println("[blockchain] Transaction History:");
        for (int i = 0; i < transactionHistory.size(); i++) {
            Transaction tx = transactionHistory.get(i);
            System.out.println("  Transaction " + (i + 1) + ":");
            System.out.println("    To Address: " + tx.toAddress);
            System.out.println("    Amount: " + tx.amount);
            System.out.println("    Transaction ID: " + tx.transactionID);
            System.out.println("    Hash Code: " + tx.hashCode);
        }
        return transactionHistory;
    }
}

class DataScienceLibrary {

    /**
     * Loads a CSV file into a Weka Instances object.
     *
     * @param csvPath Path to the CSV file.
     * @return Instances object containing the dataset.
     */
    public Instances loadCSV(String csvPath) {
        try {
            CSVLoader loader = new CSVLoader();
            loader.setSource(new File(csvPath));
            Instances data = loader.getDataSet();

            // Set class index to the last attribute if not already set
            if (data.classIndex() == -1) {
                data.setClassIndex(data.numAttributes() - 1);
            }

            // Convert string attributes to nominal
            StringToNominal stn = new StringToNominal();
            stn.setAttributeRange("first-last");
            stn.setInputFormat(data);
            data = Filter.useFilter(data, stn);

            System.out.println("[data science] Loaded data from " + csvPath);
            return data;
        } catch (IOException e) {
            throw new RuntimeException("Error loading CSV file: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Error processing CSV file: " + e.getMessage(), e);
        }
    }

    /**
     * Calculates the mean of a specified numeric attribute.
     *
     * @param data          The dataset (Instances object).
     * @param attributeName The name of the numeric attribute.
     * @return The mean value as a double.
     */
    public double calculateMean(Instances data, String attributeName) {
        validateAttribute(data, attributeName, "numeric");
        int attrIndex = data.attribute(attributeName).index();
        DescriptiveStatistics stats = new DescriptiveStatistics();

        for (int i = 0; i < data.numInstances(); i++) {
            stats.addValue(data.instance(i).value(attrIndex));
        }

        double mean = stats.getMean();
        System.out.println("[data science] Mean of '" + attributeName + "': " + mean);
        return mean;
    }

    /**
     * Calculates the median of a specified numeric attribute.
     *
     * @param data          The dataset (Instances object).
     * @param attributeName The name of the numeric attribute.
     * @return The median value as a double.
     */
    public double calculateMedian(Instances data, String attributeName) {
        validateAttribute(data, attributeName, "numeric");
        int attrIndex = data.attribute(attributeName).index();
        DescriptiveStatistics stats = new DescriptiveStatistics();

        for (int i = 0; i < data.numInstances(); i++) {
            stats.addValue(data.instance(i).value(attrIndex));
        }

        double median = stats.getPercentile(50);
        System.out.println("[data science] Median of '" + attributeName + "': " + median);
        return median;
    }

    /**
     * Calculates the standard deviation of a specified numeric attribute.
     *
     * @param data          The dataset (Instances object).
     * @param attributeName The name of the numeric attribute.
     * @return The standard deviation as a double.
     */
    public double calculateStdDev(Instances data, String attributeName) {
        validateAttribute(data, attributeName, "numeric");
        int attrIndex = data.attribute(attributeName).index();
        DescriptiveStatistics stats = new DescriptiveStatistics();

        for (int i = 0; i < data.numInstances(); i++) {
            stats.addValue(data.instance(i).value(attrIndex));
        }

        double stdDev = stats.getStandardDeviation();
        System.out.println("[data science] Standard Deviation of '" + attributeName + "': " + stdDev);
        return stdDev;
    }

    /**
     * Creates and saves a histogram for a specified numeric attribute.
     *
     * @param data          The dataset (Instances object).
     * @param attributeName The name of the numeric attribute.
     * @param outputPath    Path to save the histogram PNG file.
     */
    public void plotHistogram(Instances data, String attributeName, String outputPath) {
        validateAttribute(data, attributeName, "numeric");
        int attrIndex = data.attribute(attributeName).index();
        HistogramDataset dataset = new HistogramDataset();

        double[] values = new double[data.numInstances()];
        for (int i = 0; i < data.numInstances(); i++) {
            values[i] = data.instance(i).value(attrIndex);
        }

        dataset.addSeries(attributeName, values, 10); // 10 bins

        JFreeChart histogram = ChartFactory.createHistogram(
                "Histogram of " + attributeName,
                attributeName,
                "Frequency",
                dataset);

        try {
            ChartUtils.saveChartAsPNG(new File(outputPath), histogram, 800, 600);
            System.out.println("[data science] Histogram saved to " + outputPath);
        } catch (IOException e) {
            throw new RuntimeException("Error saving histogram: " + e.getMessage(), e);
        }
    }

    /**
     * Creates and saves a scatter plot between two specified numeric attributes.
     *
     * @param data       The dataset (Instances object).
     * @param attributeX The name of the X-axis numeric attribute.
     * @param attributeY The name of the Y-axis numeric attribute.
     * @param outputPath Path to save the scatter plot PNG file.
     */
    public void plotScatter(Instances data, String attributeX, String attributeY, String outputPath) {
        validateAttribute(data, attributeX, "numeric");
        validateAttribute(data, attributeY, "numeric");
        int xIndex = data.attribute(attributeX).index();
        int yIndex = data.attribute(attributeY).index();

        XYSeries series = new XYSeries("Data Points");
        for (int i = 0; i < data.numInstances(); i++) {
            series.add(data.instance(i).value(xIndex), data.instance(i).value(yIndex));
        }

        XYDataset dataset = new XYSeriesCollection(series);

        JFreeChart scatterPlot = ChartFactory.createScatterPlot(
                "Scatter Plot of " + attributeX + " vs " + attributeY,
                attributeX,
                attributeY,
                dataset);

        try {
            ChartUtils.saveChartAsPNG(new File(outputPath), scatterPlot, 800, 600);
            System.out.println("[data science] Scatter plot saved to " + outputPath);
        } catch (IOException e) {
            throw new RuntimeException("Error saving scatter plot: " + e.getMessage(), e);
        }
    }

    /**
     * Filters the dataset based on the specified attribute, operator, and value.
     * Supports numeric attributes and operators: ">", "<", "==", ">=", "<=".
     *
     * @param data      The original dataset.
     * @param attribute The attribute name to filter on.
     * @param operator  The operator for filtering (e.g., ">", "<", "==").
     * @param value     The value to compare against.
     * @return A new Instances object containing only the filtered data.
     */
    public Instances filterData(Instances data, String attribute, String operator, double value) {
        try {
            // Verify that the attribute exists in the dataset
            if (data.attribute(attribute) == null) {
                throw new IllegalArgumentException("Attribute '" + attribute + "' does not exist in the dataset.");
            }

            // Verify that the attribute is numeric
            if (!data.attribute(attribute).isNumeric()) {
                throw new IllegalArgumentException("Attribute '" + attribute + "' is not numeric.");
            }

            // Create a new Instances object to hold the filtered data
            Instances filteredData = new Instances(data, 0);

            // Iterate through each instance and apply the filter condition
            for (int i = 0; i < data.numInstances(); i++) {
                double attrValue = data.instance(i).value(data.attribute(attribute));
                boolean conditionMet = false;

                switch (operator) {
                    case ">":
                        conditionMet = attrValue > value;
                        break;
                    case "<":
                        conditionMet = attrValue < value;
                        break;
                    case "==":
                        conditionMet = attrValue == value;
                        break;
                    case ">=":
                        conditionMet = attrValue >= value;
                        break;
                    case "<=":
                        conditionMet = attrValue <= value;
                        break;
                    default:
                        throw new IllegalArgumentException("Unsupported operator: " + operator);
                }

                if (conditionMet) {
                    filteredData.add(data.instance(i));
                }
            }

            // Debug Statements
            System.out.println("[data science] Filtered data based on " + attribute + " " + operator + " " + value);
            System.out.println("[data science] Number of instances after filtering: " + filteredData.numInstances());

            return filteredData;

        } catch (Exception e) {
            throw new RuntimeException("Failed to filter data: " + e.getMessage(), e);
        }
    }

    /**
     * Validates that the specified attribute exists and is of the expected type.
     *
     * @param data          The dataset (Instances object).
     * @param attributeName The name of the attribute.
     * @param expectedType  The expected type of the attribute (e.g., "numeric").
     */
    private void validateAttribute(Instances data, String attributeName, String expectedType) {
        Attribute attr = data.attribute(attributeName);
        if (attr == null) {
            throw new RuntimeException("Attribute '" + attributeName + "' does not exist in the dataset.");
        }

        switch (expectedType.toLowerCase()) {
            case "numeric":
                if (!attr.isNumeric()) {
                    throw new RuntimeException("Attribute '" + attributeName + "' is not numeric.");
                }
                break;
            case "nominal":
                if (!attr.isNominal()) {
                    throw new RuntimeException("Attribute '" + attributeName + "' is not nominal.");
                }
                break;
            // Add more cases for different types if needed
            default:
                throw new RuntimeException("Unknown expected type: " + expectedType);
        }
    }

    /**
     * Evaluates a condition between two numeric values based on the operator.
     *
     * @param attrValue The attribute value from the dataset.
     * @param operator  The comparison operator.
     * @param value     The value to compare against.
     * @return True if the condition holds, otherwise false.
     */
    private boolean evaluateCondition(double attrValue, String operator, double value) {
        switch (operator) {
            case ">":
                return attrValue > value;
            case "<":
                return attrValue < value;
            case ">=":
                return attrValue >= value;
            case "<=":
                return attrValue <= value;
            case "==":
                return attrValue == value;
            case "!=":
                return attrValue != value;
            default:
                throw new RuntimeException(
                        "Unsupported operator: " + operator + ". Supported operators are >, <, >=, <=, ==, !=.");
        }
    }

    // Additional data manipulation and statistical analysis methods can be added
    // here
}

class DatabaseLibrary {
    private Connection connection;

    /**
     * Connects to the specified database using JDBC.
     *
     * @param url      The JDBC URL of the database.
     * @param user     The database username.
     * @param password The database password.
     * @return null
     */
    public Object connect(String url, String user, String password) {
        try {
            // Load the JDBC driver (e.g., MySQL)
            // Ensure the JDBC driver is in the classpath
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish the connection
            connection = DriverManager.getConnection(url, user, password);
            System.out.println("[database] Connected to database successfully.");
            return null;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("[database] JDBC Driver not found: " + e.getMessage(), e);
        } catch (SQLException e) {
            throw new RuntimeException("[database] Failed to connect to database: " + e.getMessage(), e);
        }
    }

    /**
     * Executes the given SQL query and returns the results.
     *
     * @param sql The SQL query to execute.
     * @return A list of maps representing the result set rows.
     */
    // Updated function in DatabaseLibrary class
    public Object query(String sql) {
        if (connection == null) {
            throw new RuntimeException("[database] Not connected to any database. Call db.connect() first.");
        }
        try (Statement stmt = connection.createStatement(
                ResultSet.TYPE_SCROLL_INSENSITIVE,
                ResultSet.CONCUR_READ_ONLY)) {
            boolean hasResultSet = stmt.execute(sql);
            if (hasResultSet) {
                try (ResultSet rs = stmt.getResultSet()) {
                    List<Map<String, Object>> results = new ArrayList<>();
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();

                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= columnCount; i++) {
                            row.put(meta.getColumnName(i), rs.getObject(i));
                        }
                        results.add(row);
                    }

                    System.out.println("[database] Query executed successfully. Rows fetched: " + results.size());
                    return results; // Returning the list of rows
                }
            } else {
                int updateCount = stmt.getUpdateCount();
                System.out.println("[database] Query executed successfully. Rows affected: " + updateCount);
                return updateCount; // Returning the number of rows affected
            }
        } catch (SQLException e) {
            throw new RuntimeException("[database] Query execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Closes the database connection.
     *
     * @return null
     */
    public Object close() {
        if (connection != null) {
            try {
                connection.close();
                System.out.println("[database] Connection closed.");
                connection = null;
            } catch (SQLException e) {
                throw new RuntimeException("[database] Failed to close connection: " + e.getMessage(), e);
            }
        } else {
            System.out.println("[database] No active connection to close.");
        }
        return null;
    }
}