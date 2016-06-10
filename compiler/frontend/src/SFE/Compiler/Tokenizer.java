// Tokenizer.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;


/**
 * The Tokenizer class takes an input stream and parses it into "tokens",
 * allowing the tokens to be read one at a time.
 * The tokenizer can recognize keywords, identifiers, numbers (int consts),
 * quoted strings(string consts) and various symbols.
 * The tokenizer skipps C and C++ style comments.
 */
public class Tokenizer {
  //~ Instance fields --------------------------------------------------------

  /*
   * Private data members
   */
  private StreamTokenizer tokenizer;
  private int             keywordIndex; // holds the index of the current keyword

  //~ Constructors -----------------------------------------------------------

  /**
   * Create a tokenizer that parses the given stream.
   * @param input a Reader object providing the input stream.
   */
  public Tokenizer(Reader input) {
    tokenizer = new StreamTokenizer(input);

    // set the tokenizer properties
    tokenizer.eolIsSignificant(false);
    tokenizer.lowerCaseMode(false);

    tokenizer.parseNumbers();

    /* defining the symbols of the language */
    tokenizer.ordinaryChar('.'); // for structs
    tokenizer.ordinaryChar('+');
    tokenizer.ordinaryChar('-');
    tokenizer.ordinaryChar('*');
    tokenizer.ordinaryChar('/');
    tokenizer.ordinaryChar('[');
    tokenizer.ordinaryChar(']');
    tokenizer.ordinaryChar('<');
    tokenizer.ordinaryChar('>');
    tokenizer.ordinaryChar('=');
    tokenizer.ordinaryChar('&');
    tokenizer.ordinaryChar('|');
    tokenizer.ordinaryChar('~');
    tokenizer.ordinaryChar('^');
    tokenizer.ordinaryChar('!');

    tokenizer.quoteChar('"');
    tokenizer.slashSlashComments(true);
    tokenizer.slashStarComments(true);
    tokenizer.wordChars('_', '_');
    tokenizer.wordChars('1', '0');
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Return the current line number.
   * @return the current line number of this stream tokenizer.
   */
  public int lineNumber() {
    return tokenizer.lineno();
  }

  /**
   * Indicate if there are more tokens left in the input reader.
   * @return true if there are more tokens; false otherwise.
   */
  public boolean hasMoreTokens() {
    return tokenizer.ttype != StreamTokenizer.TT_EOF;
  }

  /**
   * Parses the next token from the input stream of this tokenizer.
   * This method should only be called if hasMoreTokens() is true.
   * initially there is no current token.
   * @throws IOException - if an I/O error occurs.
   */
  public void advance() throws IOException {
    tokenizer.nextToken();
  }

  /*
   * A private method that checks if current token is a keyword.
   * @return the keyword constant if and only if current token is a keyword; -1 otherwise
   */
  private int checkKeyword() {
    for (keywordIndex = 0; keywordIndex < KEYWORDS.length;
         keywordIndex++) {
      if (tokenizer.sval.equals(KEYWORDS[keywordIndex])) {
        return keywordIndex;
      }
    }

    return -1;
  }

  /**
   * Indicate if current token is a symbol.
   * The result is true if and only if the current token is one
   * of the symbols defined in the language.
   * @return true if current token is a symbol; false otherwise
   */
  private boolean isSymbol() {
    return (tokenizer.ttype > 0) && (tokenizer.ttype != '"'); // not a quoted string
  }

  /**
   * Return the current token Type.
   * The int representing the token type is one of the following:
   * EOF, KEYWORD, SYMBOL, IDENTIFIER, INT_CONST, STRING_CONST.
   * @return an int value representing the token type.
   */
  public int tokenType() {
    if (tokenizer.ttype == StreamTokenizer.TT_WORD) {
      if (tokenizer.ttype == '"') { // a quoted string
        return STRING_CONST;
      } else if (checkKeyword() != -1) {
        return KEYWORD;
      }

      return IDENTIFIER;
    } else if (tokenizer.ttype == StreamTokenizer.TT_NUMBER) {
      return INT_CONST;
    } else if (tokenizer.ttype == StreamTokenizer.TT_EOF) {
      return EOF;
    } else if (isSymbol()) {
      return SYMBOL;
    }

    return STRING_CONST;
  }

  /**
   * Return the int representing the current keyword token.
   * This method should be called only if tokenType() is KEYWORD.
   * @return an int value representing the current keyword token.
   */
  public int keyword() {
    return keywordIndex;
  }

  /**
   * Return the current keyword token.
   * This method should be called only if tokenType() is KEYWORD.
   * @return the string of the current keyword.
   */
  public String getKeyword() {
    return KEYWORDS[keywordIndex];
  }

  /**
   * Return the char represnting the current symbol.
   * This method should be called only if tokenType() is SYMBOL.
   * @return the current symbol.
   */
  public char symbol() {
    return (char) tokenizer.ttype;
  }

  /**
   * Return the identifier in current token.
   * This method should be called only if tokenType() is IDENTIFIER.
   * @return a string containing the identifier.
   */
  public String getIdentifier() {
    return tokenizer.sval;
  }

  /**
   * Return the integer in current token.
   * This method should be called only if tokenType() is INT_CONST.
   * @return the current token integer value.
   */
  public int intVal() {
    return (int) tokenizer.nval;
  }

  /**
   * Return the string in current token.
   * This method should be called only if tokenType() is STRING_CONST.
   * @return the current token string value.
   */
  public String stringVal() {
    return tokenizer.sval;
  }

  //~ Static fields/initializers ---------------------------------------------

  /**
   * A constant indicating that the end of the stream has been read.
   */
  public static final int EOF = -1;

  /**
   * A constant indicating that a keyword token has been read.
   */
  public static final int KEYWORD = 0;

  /**
   * A constant indicating that a symbol token has been read.
   */
  public static final int SYMBOL = 1;

  /**
   * A constant indicating that an identifier token has been read.
   */
  public static final int IDENTIFIER = 3;

  /**
   * A constant indicating that a constant number token has been read.
   */
  public static final int INT_CONST = 4;

  /**
   * A constant indicating that a constant string token has been read.
   */
  public static final int STRING_CONST = 5;

  private static final String[] KEYWORDS = {
    "program", "type", "function", "boolean", "int", "float", "if", "else", "for",
    "true", "false", "const", "var", "struct", "enum", "to"
  };

  /**
   * A constant indicating that the program keyword has been read.
   */
  public static final int KW_PROGRAM = 0,

                          /**
                           * A constant indicating that the type keyword has been read.
                           */
                          KW_TYPE = KW_PROGRAM + 1,

                          /**
                           * A constant indicating that the function keyword has been read.
                           */
                          KW_FUNCTION = KW_TYPE + 1,

                          /**
                           * A constant indicating that the type boolean has been read.
                           */
                          KW_BOOLEAN = KW_FUNCTION + 1,

                          /**
                           * A constant indicating that the int keyword has been read.
                           */
                          KW_INT = KW_BOOLEAN + 1,

                          /**
                           * A constant indicating that the float keyword has been read.
                           */
                          KW_FLOAT = KW_INT + 1,

                          /**
                           * A constant indicating that the if keyword has been read.
                           */
                          KW_IF = KW_FLOAT + 1,

                          /**
                           * A constant indicating that the else keyword has been read.
                           */
                          KW_ELSE = KW_IF + 1,

                          /**
                           * A constant indicating that the for keyword has been read.
                           */
                          KW_FOR = KW_ELSE+1,

                          /**
                           * A constant indicating that the true keyword has been read.
                           */
                          KW_TRUE = KW_FOR+1,

                          /**
                           * A constant indicating that the false keyword has been read.
                           */
                          KW_FALSE = KW_TRUE + 1,

                          /**
                           * A constant indicating that the const keyword has been read.
                           */
                          KW_CONST = KW_FALSE+1,

                          /**
                           * A constant indicating that the var keyword has been read.
                           */
                          KW_VAR = KW_CONST+1,

                          /**
                           * A constant indicating that the struct keyword has been read.
                           */
                          KW_STRUCT = KW_VAR+1,

                          /**
                           * A constant indicating that the enum keyword has been read.
                           */
                          KW_ENUM = KW_STRUCT+1,

                          /**
                           * A constant indicating that the to keyword has been read.
                           */
                          KW_TO = KW_ENUM + 1;
}
