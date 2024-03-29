import java.io.File;
import java.util.regex.Pattern;

/**
 * The compilation engine gets its input from a {@link JackTokenizer} and emits its output to an output file. The output
 * is generated by a series of <code>compile</code><i>xxx</i> routines, each designed to handle the compilation of a
 * specific Jack language construct <i>xxx</i>. The contract between routines is that each <code>compile</code><i>xxx</i>
 * routine should get from the input, and handle, all the tokens that make up <i>xxx</i>, advance the tokenizer exactly
 * beyond these tokens, and output the parsing of <i>xxx</i>. As a rule, each <code>compile</code><i>xxx</i> is called
 * only if the current token is <i>xxx</i>.
 *
 * @author Maarten Derks
 */
class CompilationEngine {

    private final JackTokenizer jt;
    private final SymbolTable cst, sst;
    private final VMWriter vmw;

    private final String identifier = "[a-zA-Z_][a-zA-Z0-9_]*";
    private final String className = identifier;
    private final String type = "int|char|boolean|" + className;
    private final String varName = identifier;
    private final String subroutineName = identifier;
    private final String unaryOp = "-|~";

    private String currentClassName;
    private String currentSubroutineName;
    private String currentSubroutineType;
    private int ifIndex;
    private int whileIndex;

    /**
     * Creates a new compilation engine with the given input and output.
     *
     * @param in  Input stream/file
     * @param out Output stream/file
     */
    CompilationEngine(File in, File out) throws Exception {
        jt = new JackTokenizer(in);
        vmw = new VMWriter(out);
        cst = new SymbolTable();
        sst = new SymbolTable();
        jt.advance();
    }

    /**
     * Compiles a complete class.
     */
    void compileClass() {
        process("class");
        currentClassName = process(className);
        process("{");
        while (jt.tokenType() == TokenType.KEYWORD &&
                (jt.keyWord() == Keyword.STATIC || jt.keyWord() == Keyword.FIELD)) {
            compileClassVarDec();
        }
        while (jt.tokenType() == TokenType.KEYWORD &&
                (jt.keyWord() == Keyword.CONSTRUCTOR || jt.keyWord() == Keyword.FUNCTION || jt.keyWord() == Keyword.METHOD)) {
            compileSubroutine();
        }
        process("}");
        vmw.close();
    }

    /**
     * Compiles a static variable declaration, or a field declaration.
     */
    private void compileClassVarDec() {
        String kind = process("static|field");
        String t = process(type);
        cst.define(jt.identifier(), t, Kind.valueOf(kind.toUpperCase()));
        process(varName);
        while (jt.tokenType() == TokenType.SYMBOL && jt.symbol() == ',') {
            process(",");
            cst.define(jt.identifier(), t, Kind.valueOf(kind.toUpperCase()));
            process(varName);
        }
        process(";");
    }

    /**
     * Compiles a complete method, function, or constructor.
     */
    private void compileSubroutine() {
        sst.reset();
        ifIndex = whileIndex = 0;
        currentSubroutineType = process("constructor|function|method");
        if (Pattern.matches("method", currentSubroutineType)) {
            sst.define("this", currentClassName, Kind.ARG);
        }
        process("void|" + type);
        currentSubroutineName = process(subroutineName);
        process("(");
        compileParameterList();
        process(")");
        compileSubroutineBody();
    }

    /**
     * Compiles a (possibly empty) parameter list. Does not handle the enclosing parentheses tokens ( and ).
     */
    private void compileParameterList() {
        if ((jt.tokenType() == TokenType.KEYWORD && (jt.keyWord() == Keyword.INT || jt.keyWord() == Keyword.BOOLEAN)) || (jt.tokenType() == TokenType.IDENTIFIER)) {
            String t = process(type);
            sst.define(jt.identifier(), t, Kind.ARG);
            process(varName);
        }
        while (jt.tokenType() == TokenType.SYMBOL && jt.symbol() == ',') {
            process(",");
            String t = process(type);
            sst.define(jt.identifier(), t, Kind.ARG);
            process(varName);
        }
    }

    /**
     * Compiles a subroutine's body.
     */
    private void compileSubroutineBody() {
        process("{");
        while (jt.tokenType() == TokenType.KEYWORD && jt.keyWord() == Keyword.VAR) {
            compileVarDec();
        }
        vmw.writeFunction(currentClassName + "." + currentSubroutineName, sst.varCount(Kind.VAR));
        switch (currentSubroutineType) {
            case "constructor":
                vmw.writePush(Segment.CONSTANT, cst.varCount(Kind.FIELD));
                vmw.writeCall("Memory.alloc", 1);
                vmw.writePop(Segment.POINTER, 0);
                break;
            case "method":
                vmw.writePush(Segment.ARGUMENT, 0);
                vmw.writePop(Segment.POINTER, 0);
                break;
        }
        compileStatements();
        process("}");
    }

    /**
     * Compiles a <code>var</code> declaration.
     */
    private void compileVarDec() {
        process("var");
        String t = process(type);
        sst.define(jt.identifier(), t, Kind.VAR);
        process(varName);
        while (jt.tokenType() == TokenType.SYMBOL && jt.symbol() == ',') {
            process(",");
            sst.define(jt.identifier(), t, Kind.VAR);
            process(varName);
        }
        process(";");
    }

    /**
     * Compiles a sequence of statements. Does not handle the enclosing curly bracket tokens { and }.
     */
    private void compileStatements() {
        while (jt.tokenType() == TokenType.KEYWORD && (jt.keyWord() == Keyword.LET || jt.keyWord() == Keyword.IF || jt.keyWord() == Keyword.WHILE || jt.keyWord() == Keyword.DO || jt.keyWord() == Keyword.RETURN)) {
            switch (jt.keyWord()) {
                case LET: compileLet(); break;
                case IF: compileIf(); break;
                case WHILE: compileWhile(); break;
                case DO: compileDo(); break;
                case RETURN: compileReturn(); break;
            }
        }
    }

    /**
     * Compiles a <code>let</code> statement.
     */
    private void compileLet() {
        process("let");
        String var = process(varName);
        if (jt.tokenType() == TokenType.SYMBOL && jt.symbol() == '[') {
            process("[");
            compileExpression();
            process("]");
            switch (sst.kindOf(var)) {
                case ARG:
                    vmw.writePush(Segment.ARGUMENT, sst.indexOf(var));
                    break;
                case VAR:
                    vmw.writePush(Segment.LOCAL, sst.indexOf(var));
                    break;
            }
            vmw.writeArithmetic(Command.ADD);
            process("=");
            compileExpression();
            vmw.writePop(Segment.TEMP, 0);
            vmw.writePop(Segment.POINTER,1);
            vmw.writePush(Segment.TEMP, 0);
            vmw.writePop(Segment.THAT, 0);
        } else {
            process("=");
            compileExpression();
            switch (sst.kindOf(var)) {
                case VAR:
                    vmw.writePop(Segment.LOCAL, sst.indexOf(var));
                    break;
                case ARG:
                    vmw.writePop(Segment.ARGUMENT, sst.indexOf(var));
                case NONE:
                    switch (cst.kindOf(var)) {
                        case FIELD:
                            vmw.writePop(Segment.THIS, cst.indexOf(var));
                            break;
                        case STATIC:
                            vmw.writePop(Segment.STATIC, cst.indexOf(var));
                            break;
                    }
                    break;
            }
        }
        process(";");
    }

    /**
     * Compiles an <code>if</code> statement, possibly with a trailing <code>else</code> clause.
     */
    private void compileIf() {
        int i = ifIndex;
        ifIndex++;
        process("if");
        process("(");
        compileExpression();
        vmw.writeIf("IF_TRUE" + i);
        vmw.writeGoto("IF_FALSE" + i);
        vmw.writeLabel("IF_TRUE" + i);
        process(")");
        process("{");
        compileStatements();
        process("}");
        if (jt.tokenType() == TokenType.KEYWORD && jt.keyWord() == Keyword.ELSE) {
            vmw.writeGoto("IF_END" + i);
            vmw.writeLabel("IF_FALSE" + i);
            process("else");
            process("{");
            compileStatements();
            process("}");
            vmw.writeLabel("IF_END" + i);
        } else {
            vmw.writeLabel("IF_FALSE" + i);
        }
    }

    /**
     * Compiles a <code>while</code> statement.
     */
    private void compileWhile() {
        int i = whileIndex;
        vmw.writeLabel("WHILE_EXP" + i);
        process("while");
        process("(");
        compileExpression();
        process(")");
        vmw.writeArithmetic(Command.NOT);
        vmw.writeIf("WHILE_END" + i);
        process("{");
        whileIndex++;
        compileStatements();
        vmw.writeGoto("WHILE_EXP" + i);
        process("}");
        vmw.writeLabel("WHILE_END" + i);
    }

    /**
     * Compiles a <code>do</code> statement.
     */
    private void compileDo() {
        process("do");
        String name = process(identifier);
        String type = "";
        String subroutine = "";
        if (sst.kindOf(name) != Kind.NONE) {
            type = sst.typeOf(name);
            vmw.writePush(Segment.LOCAL, 0);
        } else {
            if (cst.kindOf(name) != Kind.NONE) {
                type = cst.typeOf(name);
                vmw.writePush(Segment.THIS, cst.indexOf(name));
            } else if (Character.isUpperCase(name.charAt(0))) {
                type = name;
            } else {
                type = currentClassName;
            }
        }
        if (jt.tokenType() == TokenType.SYMBOL && jt.symbol() == '.') {
            process(".");
            subroutine = process(subroutineName);
        } else {
            vmw.writePush(Segment.POINTER, 0);
            subroutine = name;
        }
        process("(");
        int nVars = compileExpressionList();
        process(")");
        process(";");

        String call = type + "." + subroutine;

        if (Character.isUpperCase(name.charAt(0))) {
            vmw.writeCall(call, nVars);
        } else {
            vmw.writeCall(call, nVars + 1);
        }
        vmw.writePop(Segment.TEMP, 0);
    }

    /**
     * Compiles a <code>return</code> statement.
     */
    private void compileReturn() {
        process("return");
        if (jt.tokenType() == TokenType.SYMBOL && jt.symbol() == ';') {
            vmw.writePush(Segment.CONSTANT, 0);
        } else {
            compileExpression();
        }
        process(";");
        vmw.writeReturn();
    }

    /**
     * Compiles an expression.
     */
    private void compileExpression() {
        compileTerm();
        while (jt.tokenType() == TokenType.SYMBOL && (
                jt.symbol() == '+' ||
                        jt.symbol() == '-' ||
                        jt.symbol() == '*' ||
                        jt.symbol() == '/' ||
                        jt.symbol() == '&' ||
                        jt.symbol() == '|' ||
                        jt.symbol() == '<' ||
                        jt.symbol() == '>' ||
                        jt.symbol() == '='
        )) {
            char op = jt.symbol();
            jt.advance();
            compileTerm();
            switch (op) {
                case '+':
                    vmw.writeArithmetic(Command.ADD);
                    break;
                case '-':
                    vmw.writeArithmetic(Command.SUB);
                    break;
                case '*':
                    vmw.writeCall("Math.multiply", 2);
                    break;
                case '/':
                    vmw.writeCall("Math.divide", 2);
                    break;
                case '<':
                    vmw.writeArithmetic(Command.LT);
                    break;
                case '>':
                    vmw.writeArithmetic(Command.GT);
                    break;
                case '=':
                    vmw.writeArithmetic(Command.EQ);
                    break;
                case '&':
                    vmw.writeArithmetic(Command.AND);
                    break;
                case '|':
                    vmw.writeArithmetic(Command.OR);
                    break;
            }
        }
    }

    /**
     * Compiles a (possibly empty) comma-separated list of expressions.
     *
     * @return  the number of expressions in the list
     */
    private int compileExpressionList() {
        int numberOfExpressions = 0;
        while (jt.symbol() != ')') {
            compileExpression();
            numberOfExpressions++;
            while (jt.tokenType() == TokenType.SYMBOL && jt.symbol() == ',') {
                process(",");
                compileExpression();
                numberOfExpressions++;
            }
        }
        return numberOfExpressions;
    }

    /**
     * Compiles a <i>term</i>. If the current token is an <i>identifier</i>, the routine must distinguish between a
     * <i>variable</i>, an <i>array entry</i>, or a <i>subroutine call</i>. A single look-ahead token, which may be one
     * of "[", "(", or ".", suffices to distinguish between the possibilities. Any other token is not part of this term
     * and should not be advanced over.
     */
    private void compileTerm() {
        System.out.println(jt.identifier());
        switch (jt.tokenType()) {
            case INT_CONST:
                vmw.writePush(Segment.CONSTANT, jt.intVal());
                jt.advance();
                break;
            case STRING_CONST:
                vmw.writePush(Segment.CONSTANT, jt.stringVal().length());
                vmw.writeCall("String.new", 1);
                for (int i = 0; i < jt.stringVal().length(); i++) {
                    vmw.writePush(Segment.CONSTANT, jt.stringVal().charAt(i));
                    vmw.writeCall("String.appendChar", 2);
                }
                jt.advance();
                break;
            case KEYWORD:
                switch (jt.keyWord()) {
                    case TRUE:
                        vmw.writePush(Segment.CONSTANT, 0);
                        vmw.writeArithmetic(Command.NOT);
                        break;
                    case FALSE:
                    case NULL:
                        vmw.writePush(Segment.CONSTANT, 0);
                        break;
                    case THIS:
                        vmw.writePush(Segment.POINTER, 0);
                        break;
                }
                jt.advance();
                break;
            case IDENTIFIER:
                String v = jt.identifier();
                jt.advance();
                if (jt.tokenType() == TokenType.SYMBOL && jt.symbol() == '[') {
                    process("[");
                    compileExpression();
                    process("]");
                    vmw.writePush(Segment.LOCAL, sst.indexOf(v));
                    vmw.writeArithmetic(Command.ADD);
                    vmw.writePop(Segment.POINTER, 1);
                    vmw.writePush(Segment.THAT, 0);
                } else if (jt.tokenType() == TokenType.SYMBOL && jt.symbol() == '.') {
                    process(".");
                    String subroutine = process(subroutineName);
                    process("(");
                    int nVars = compileExpressionList();
                    process(")");
                    if (sst.kindOf(v) != Kind.NONE) {
                        vmw.writePush(Segment.THIS, sst.indexOf(v));
                        v = sst.typeOf(v);
                        nVars++;
                    } else if (cst.kindOf(v) != Kind.NONE) {
                        vmw.writePush(Segment.THIS, cst.indexOf(v));
                        v = cst.typeOf(v);
                        nVars++;
                    }
                    vmw.writeCall(v + "." + subroutine, nVars);
                } else {
                    switch (sst.kindOf(v)) {
                        case ARG:
                            vmw.writePush(Segment.ARGUMENT, sst.indexOf(v));
                            break;
                        case VAR:
                            vmw.writePush(Segment.LOCAL, sst.indexOf(v));
                            break;
                        case NONE:
                            switch (cst.kindOf(v)) {
                                case FIELD:
                                    vmw.writePush(Segment.THIS, cst.indexOf(v));
                                    break;
                                case STATIC:
                                    vmw.writePush(Segment.STATIC, cst.indexOf(v));
                                    break;
                            }
                            break;
                    }
                }
                break;
            case SYMBOL:
                switch (jt.symbol()) {
                    case '(':
                        process("(");
                        compileExpression();
                        process(")");
                        break;
                    case '-':
                        process(unaryOp);
                        compileTerm();
                        vmw.writeArithmetic(Command.NEG);
                        break;
                    case '~':
                        process(unaryOp);
                        compileTerm();
                        vmw.writeArithmetic(Command.NOT);
                        break;
                }
                break;
        }
    }

    /**
     * A helper routine that handles the current token, and advances to get the next token.
     *
     * @param str
     */
    private String process(String str) {
        String token = jt.identifier();
        switch (str) {
            case "{":
            case "(":
            case ")":
            case "[":
                str = "\\".concat(str);
        }
        if (Pattern.matches(str, token));
        if (jt.hasMoreTokens()) jt.advance();
        return token;
    }
}