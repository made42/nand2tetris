import java.io.File;
import java.util.Scanner;
import java.util.regex.Pattern;

class JackTokenizer {

    Scanner sc;
    char nextChar;
    String token;

    String keywordRegex = "class|constructor|function|method|field|static|var|int|char|boolean|void|true|false|null|this|let|do|if|else|while|return";
    String symbolRegex = "\\{|}|\\(|\\)|\\[|]|\\.|,|;|\\+|-|\\*|/|&|\\||<|>|=|~";
    String integerConstantRegex = "^(0|[1-9]\\d*)$";
    String stringConstantRegex = "\".*?\"";
    String identifierRegex = "[a-zA-Z_][a-zA-Z0-9_]*";

    /**
     * Ignores all comments and white space in the input stream, and serializes it into Jack-language tokens.
     * The token types are specified according to the Jack grammar.
     *
     * @param file
     * @throws Exception
     */
    JackTokenizer(File file) throws Exception {
        sc = new Scanner(file);
        sc.useDelimiter("");
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
         if (Pattern.matches(symbolRegex, String.valueOf(nextChar))) {
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

            if (!stringConstant && (nextChar == '(' || nextChar == ')' || nextChar == '[' || nextChar == ']' || nextChar == ',' || nextChar == ';' || nextChar == '.')) {
                token = potentialToken;
                return;
            }
        }

        // ignore the token if it's a comment
        if (potentialToken.startsWith("//")) {
            sc.nextLine();
            if (sc.hasNext()) {
                advance();
                return;
            }
        }

        if (potentialToken.startsWith("/**")) {
            String nextLine = sc.nextLine();
            while (!nextLine.contains("*/")) {
                nextLine = sc.nextLine();
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
        if (Pattern.matches(keywordRegex, token)) return TokenType.KEYWORD;
        if (Pattern.matches(symbolRegex, token)) return TokenType.SYMBOL;
        if (Pattern.matches(integerConstantRegex, token)) return TokenType.INT_CONST;
        if (Pattern.matches(stringConstantRegex, token)) return TokenType.STRING_CONST;
        if (Pattern.matches(identifierRegex, token)) return TokenType.IDENTIFIER;
        return null;
    }

    /**
     * Returns the keyword which is the current token, as a constant.
     * This method should be called only if {@link #tokenType() tokenType} is {@link TokenType#KEYWORD KEYWORD}.
     *
     * @return  the keyword which is the current token
     * @see     KeyWord
     */
    KeyWord keyWord() {
        switch (token) {
            case "class": return KeyWord.CLASS;
            case "method": return KeyWord.METHOD;
            case "function": return KeyWord.FUNCTION;
            case "constructor": return KeyWord.CONSTRUCTOR;
            case "int": return KeyWord.INT;
            case "boolean": return KeyWord.BOOLEAN;
            case "char": return KeyWord.CHAR;
            case "void": return KeyWord.VOID;
            case "var": return KeyWord.VAR;
            case "static": return KeyWord.STATIC;
            case "field": return KeyWord.FIELD;
            case "let": return KeyWord.LET;
            case "do": return KeyWord.DO;
            case "if": return KeyWord.IF;
            case "else": return KeyWord.ELSE;
            case "while": return KeyWord.WHILE;
            case "return": return KeyWord.RETURN;
            case "true": return KeyWord.TRUE;
            case "false": return KeyWord.FALSE;
            case "null": return KeyWord.NULL;
            case "this": return KeyWord.THIS;
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
