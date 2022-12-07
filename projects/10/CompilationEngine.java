import java.io.File;
import java.io.PrintWriter;

class CompilationEngine {

    JackTokenizer tokenizer;
    PrintWriter pw;
    String indentation;

    /**
     * Creates a new compilation engine with the given input and output.
     *
     * @param in  Input stream/file
     * @param out Output stream/file
     */
    CompilationEngine(File in, File out) throws Exception {
        tokenizer = new JackTokenizer(in);
        pw = new PrintWriter(out);
        indentation = "";
        tokenizer.advance();
    }

    /**
     * Compiles a complete class.
     */
    void compileClass() {
        print("<class>");
        process("class");
        process();
        process("{");
        while (tokenizer.tokenType() == TokenType.KEYWORD &&
                (tokenizer.keyWord() == KeyWord.STATIC || tokenizer.keyWord() == KeyWord.FIELD)) {
            compileClassVarDec();
        }
        while (tokenizer.tokenType() == TokenType.KEYWORD &&
                (tokenizer.keyWord() == KeyWord.CONSTRUCTOR || tokenizer.keyWord() == KeyWord.FUNCTION || tokenizer.keyWord() == KeyWord.METHOD)) {
            compileSubroutine();
        }
        process("}");
        print("</class>");
        pw.close();
    }

    /**
     * Compiles a static variable declaration, or a field declaration.
     */
    void compileClassVarDec() {
        print("<classVarDec>");
        process();
        process();
        process();
        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            process(",");
            process();
        }
        process(";");
        print("</classVarDec>");
    }

    /**
     * Compiles a complete method, function, or constructor.
     */
    void compileSubroutine() {
        print("<subroutineDec>");
        switch (tokenizer.token) {
            case "constructor":
            case "function":
            case "method":
                process();
                break;
        }
        switch (tokenizer.token) {
            case "void":
                process();
                break;
            default:
                switch (tokenizer.token) {
                    case "int":
                    case "char":
                    case "boolean":
                        process();
                        break;
                    default:
                        process();
                        break;
                }
        }
        process();
        process("(");
        compileParameterList();
        process(")");
        compileSubroutineBody();
        print("</subroutineDec>");
    }

    /**
     * Compiles a (possibly empty) parameter list. Does not handle the enclosing parentheses tokens ( and ).
     */
    void compileParameterList() {
        print("<parameterList>");
        if (tokenizer.tokenType() == TokenType.KEYWORD && (tokenizer.keyWord() == KeyWord.INT || tokenizer.keyWord() == KeyWord.BOOLEAN)) {
            process();
        }
        if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
            process();
        }
        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            process(",");
            if (tokenizer.tokenType() == TokenType.KEYWORD && (tokenizer.keyWord() == KeyWord.INT || tokenizer.keyWord() == KeyWord.BOOLEAN)) {
                process();
            }
            if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
                process();
            }
        }
        print("</parameterList>");
    }

    /**
     * Compiles a subroutine's body.
     */
    void compileSubroutineBody() {
        print("<subroutineBody>");
        process("{");
        while (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord() == KeyWord.VAR) {
            compileVarDec();
        }
        compileStatements();
        process("}");
        print("</subroutineBody>");
    }

    /**
     * Compiles a <code>var</code> declaration.
     */
    void compileVarDec() {
        print("<varDec>");
        process("var");
        process();
        process();
        while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
            process(",");
            process();
        }
        process(";");
        print("</varDec>");
    }

    /**
     * Compiles a sequence of statements. Does not handle the enclosing curly bracket tokens { and }.
     */
    void compileStatements() {
        print("<statements>");
        while (tokenizer.tokenType() == TokenType.KEYWORD && (tokenizer.keyWord() == KeyWord.LET || tokenizer.keyWord() == KeyWord.IF || tokenizer.keyWord() == KeyWord.WHILE || tokenizer.keyWord() == KeyWord.DO || tokenizer.keyWord() == KeyWord.RETURN)) {
            switch (tokenizer.keyWord()) {
                case LET: compileLet(); break;
                case IF: compileIf(); break;
                case WHILE: compileWhile(); break;
                case DO: compileDo(); break;
                case RETURN: compileReturn(); break;
            }
        }
        print("</statements>");
    }

    /**
     * Compiles a <code>let</code> statement.
     */
    void compileLet() {
        print("<letStatement>");
        process("let");
        process();
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
            process("[");
            compileExpression();
            process("]");
        }
        process("=");
        compileExpression();
        process(";");
        print("</letStatement>");
    }

    /**
     * Compiles an <code>if</code> statement, possibly with a trailing <code>else</code> clause.
     */
    void compileIf() {
        print("<ifStatement>");
        process("if");
        process("(");
        compileExpression();
        process(")");
        process("{");
        compileStatements();
        process("}");
        if (tokenizer.tokenType() == TokenType.KEYWORD && tokenizer.keyWord() == KeyWord.ELSE) {
            process("else");
            process("{");
            compileStatements();
            process("}");
        }
        print("</ifStatement>");
    }

    /**
     * Compiles a <code>while</code> statement.
     */
    void compileWhile() {
        print("<whileStatement>");
        process("while");
        process("(");
        compileExpression();
        process(")");
        process("{");
        compileStatements();
        process("}");
        print("</whileStatement>");
    }

    /**
     * Compiles a <code>do</code> statement.
     */
    void compileDo() {
        print("<doStatement>");
        process("do");
        if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
            process();
        }
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '.') {
            process(".");
        }
        if (tokenizer.tokenType() == TokenType.IDENTIFIER) {
            process();
        }
        process("(");
        compileExpressionList();
        process(")");
        process(";");
        print("</doStatement>");
    }

    /**
     * Compiles a <code>return</code> statement.
     */
    void compileReturn() {
        print("<returnStatement>");
        process("return");
        if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ';') {
            process(";");
        } else {
            compileExpression();
            process(";");
        }
        print("</returnStatement>");
    }

    /**
     * Compiles an expression.
     */
    void compileExpression() {
        print("<expression>");
        compileTerm();
        while (tokenizer.tokenType() == TokenType.SYMBOL && (
                tokenizer.symbol() == '+' ||
                        tokenizer.symbol() == '-' ||
                        tokenizer.symbol() == '*' ||
                        tokenizer.symbol() == '/' ||
                        tokenizer.symbol() == '&' ||
                        tokenizer.symbol() == '|' ||
                        tokenizer.symbol() == '<' ||
                        tokenizer.symbol() == '>' ||
                        tokenizer.symbol() == '='
        )) {

            switch (tokenizer.symbol()) {
                case '<': pw.println(indentation + "<symbol> " + "&lt;" + " </symbol>"); break;
                case '>': pw.println(indentation + "<symbol> " + "&gt;" + " </symbol>"); break;
                case '&': pw.println(indentation + "<symbol> " + "&amp;" + " </symbol>"); break;
                default: pw.println(indentation + "<symbol> " + tokenizer.symbol() + " </symbol>"); break;
            }
            tokenizer.advance();
            compileTerm();
        }
        print("</expression>");
    }

    /**
     * Compiles a (possibly empty) comma-separated list of expressions.
     */
    void compileExpressionList() {
        print("<expressionList>");
        while (tokenizer.symbol() != ')') {
            compileExpression();
            while (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == ',') {
                process(",");
                compileExpression();
            }
        }
        print("</expressionList>");
    }

    /**
     * Compiles a <i>term</i>. If the current token is an <i>identifier</i>, the routine must distinguish between a
     * <i>variable</i>, an <i>array entry</i>, or a <i>subroutine call</i>. A single look-ahead token, which may be one
     * of "[", "(", or ".", suffices to distinguish between the possibilities. Any other token is not part of this term
     * and should not be advanced over.
     */
    void compileTerm() {
        print("<term>");
        switch (tokenizer.tokenType()) {
            case INT_CONST:
            case STRING_CONST:
                process();
                break;
            case KEYWORD:
                switch (tokenizer.keyWord()) {
                    case TRUE:
                    case FALSE:
                    case NULL:
                    case THIS:
                        process();
                        break;
                }
                break;
            case IDENTIFIER:
                process();
                if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '[') {
                    process("[");
                    compileExpression();
                    process("]");
                }
                if (tokenizer.tokenType() == TokenType.SYMBOL && tokenizer.symbol() == '.') {
                    process(".");
                    process();
                    process("(");
                    compileExpressionList();
                    process(")");
                }
                break;
            case SYMBOL:
                switch (tokenizer.symbol()) {
                    case '(':
                        process("(");
                        compileExpression();
                        process(")");
                        break;
                    case '-':
                    case '~':
                        process();
                        compileTerm();
                        break;
                }
                break;
        }
        print("</term>");
    }

    void print(String str) {
        if (str.startsWith("</")) indentation = indentation.substring(0, indentation.length() - 2);
        pw.println(indentation + str);
        if (!str.startsWith("</")) indentation += "  ";
    }

    void process() {
        printXMLToken(tokenizer.token);
        if (tokenizer.hasMoreTokens()) tokenizer.advance();
    }

    void process(String str) {
        if (tokenizer.token.equals(str)) printXMLToken(str);
        else {
            // TODO: print error message
        }
        if (tokenizer.hasMoreTokens()) tokenizer.advance();
    }

    void printXMLToken(String str) {
        switch (tokenizer.tokenType()) {
            case KEYWORD:
                pw.println(indentation + "<keyword> " + tokenizer.keyWord().toString().toLowerCase() + " </keyword>");
                break;
            case SYMBOL:
                pw.println(indentation + "<symbol> " + tokenizer.symbol() + " </symbol>");
                break;
            case INT_CONST:
                pw.println(indentation + "<integerConstant> " + tokenizer.intVal() + " </integerConstant>");
                break;
            case STRING_CONST:
                pw.println(indentation + "<stringConstant> " + tokenizer.stringVal() + " </stringConstant>");
                break;
            case IDENTIFIER:
                pw.println(indentation + "<identifier> " + tokenizer.identifier() + " </identifier>");
                break;
        }
    }
}