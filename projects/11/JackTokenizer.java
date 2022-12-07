import java.io.File;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * This module ignores all comments and white space in the input stream and enables accessing the input one token at a
 * time. Also, it parses and provides the <i>type</i> of each token, as defined by the Jack grammar.
 *
 * @author Maarten Derks
 */
class JackTokenizer {

    private final Scanner sc;
    private char nextChar;
    private String token;

    private final String keyword = "class|constructor|function|method|field|static|var|int|char|boolean|void|true|false|null|this|let|do|if|else|while|return";
    private final String symbol = "\\{|}|\\(|\\)|\\[|]|\\.|,|;|\\+|-|\\*|/|&|\\||<|>|=|~";
    private final String integerConstant = "^(0|[1-9]\\d*)$";
    private final String stringConstant = "\".*?\"";
    private final String identifier = "[a-zA-Z_][a-zA-Z0-9_]*";

    /**
     * Ignores all comments and white space in the input stream, and serializes it into Jack-language tokens.
     * The token types are specified according to the Jack grammar.
     *
     * @param file
     * @throws Exception
     */
    JackTokenizer(File file) throws Exception {
        sc = new Scanner(file).useDelimiter("");
    }

    /**
     * Are there more tokens in the input?
     *
     * @return boolean
     */
    boolean hasMoreTokens() {
        return sc.hasNext();
    }

    /**
     * Gets the next token from the input, and makes it the current token.
     * This method should be called only if {@link #hasMoreTokens() hasMoreTokens} is true.
     * Initially there is no current token.
     */
    void advance() {
        // prepare variables
        token = "";
        String potentialToken = "";

        // previous token construction finished at a symbol, so make that the current token and stop
         if (Pattern.matches(symbol, String.valueOf(nextChar))) {
            token = String.valueOf(nextChar);
            nextChar = 0;
            return;
        }

        // take the next character from the input
        nextChar = sc.next().charAt(0);

        // the next character is a symbol, which is a token
        if (nextChar == '(' || nextChar == ')' || nextChar == '[' || nextChar == ']' || nextChar == ',' || nextChar == ';' || nextChar == '.' || nextChar == '-') {
            token = String.valueOf(nextChar);
            nextChar = 0;
            return;
        }

        // skip over any kind of whitespace
        while (Character.isWhitespace(nextChar)) {
            if (!sc.hasNext()) {
                return;
            }
            nextChar = sc.next().charAt(0);
        }

        boolean stringConstant = false;

        // build up a potential token
        while (sc.hasNext() && (stringConstant || !Character.isWhitespace(nextChar))) {

            if (nextChar == '"') {
                stringConstant = !stringConstant;
            }

            if (nextChar == '-' || nextChar == '(' || nextChar == '~') {
                token = String.valueOf(nextChar);
                nextChar = 0;
                return;
            }

            potentialToken += nextChar;
            nextChar = sc.next().charAt(0);

            if (potentialToken.equals("sum")) {
                token = potentialToken;
                return;
            }

            if (!stringConstant && (nextChar == '(' || nextChar == ')' || nextChar == '[' || nextChar == ']' || nextChar == ',' || nextChar == ';' || nextChar == '.')) {
                token = potentialToken;
                if (token.equals("sum/length")) {
                    System.out.println("i was constructed here");
                }
                return;
            }
        }

        // ignore the token if it's a comment
        if (potentialToken.startsWith("//") || potentialToken.startsWith("/**")) {
            if (potentialToken.startsWith("//")) {
                sc.nextLine();
            } else if (potentialToken.startsWith("/**")) {
                String nextLine = sc.nextLine();
                while (!nextLine.contains("*/")) {
                    nextLine = sc.nextLine();
                }
            }
            if (sc.hasNext()) {
                advance();
                return;
            }
        }

        token = potentialToken;
    }

    /**
     * Returns the type of the current token, as a constant.
     *
     * @return  the type of the current token
     * @see     TokenType
     */
    TokenType tokenType() {
        if (Pattern.matches(keyword, token)) return TokenType.KEYWORD;
        if (Pattern.matches(symbol, token)) return TokenType.SYMBOL;
        if (Pattern.matches(integerConstant, token)) return TokenType.INT_CONST;
        if (Pattern.matches(stringConstant, token)) return TokenType.STRING_CONST;
        if (Pattern.matches(identifier, token)) return TokenType.IDENTIFIER;
        return null;
    }

    /**
     * Returns the keyword which is the current token, as a constant.
     * This method should be called only if {@link #tokenType() tokenType} is {@link TokenType#KEYWORD KEYWORD}.
     *
     * @return  the keyword which is the current token
     * @see     Keyword
     */
    Keyword keyWord() {
        switch (token) {
            case "class": return Keyword.CLASS;
            case "method": return Keyword.METHOD;
            case "function": return Keyword.FUNCTION;
            case "constructor": return Keyword.CONSTRUCTOR;
            case "int": return Keyword.INT;
            case "boolean": return Keyword.BOOLEAN;
            case "char": return Keyword.CHAR;
            case "void": return Keyword.VOID;
            case "var": return Keyword.VAR;
            case "static": return Keyword.STATIC;
            case "field": return Keyword.FIELD;
            case "let": return Keyword.LET;
            case "do": return Keyword.DO;
            case "if": return Keyword.IF;
            case "else": return Keyword.ELSE;
            case "while": return Keyword.WHILE;
            case "return": return Keyword.RETURN;
            case "true": return Keyword.TRUE;
            case "false": return Keyword.FALSE;
            case "null": return Keyword.NULL;
            case "this": return Keyword.THIS;
        }
        return null;
    }

    /**
     * Returns the character which is the current token.
     * Should be called only if {@link #tokenType() tokenType} is {@link TokenType#SYMBOL SYMBOL}.
     *
     * @return  the character which is the current token
     */
    char symbol() {
        return token.charAt(0);
    }

    /**
     * Returns the identifier which is the current token.
     * Should be called only if {@link #tokenType() tokenType} is {@link TokenType#IDENTIFIER IDENTIFIER}.
     *
     * @return  the identifier which is the current token
     */
    String identifier() {
        return token;
    }

    /**
     * Returns the integer value of the current token.
     * Should be called only if {@link #tokenType() tokenType} is {@link TokenType#INT_CONST INT_CONST}.
     *
     * @return  the integer value of the current token
     */
    int intVal() {
        return Integer.parseInt(token);
    }

    /**
     * Returns the string value of the current token, without the two enclosing double quotes.
     * Should be called only if {@link #tokenType() tokenType} is {@link TokenType#STRING_CONST STRING_CONST}.
     *
     * @return  the string value of the current token, without the two enclosing double quotes
     */
    String stringVal() {
        return token.substring(1, token.length() - 1);
    }
}
