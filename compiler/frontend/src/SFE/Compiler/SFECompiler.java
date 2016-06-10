// SFECompiler.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.io.IOException;
import java.io.Reader;
import java.text.ParseException;
import java.util.EmptyStackException;
import java.util.Enumeration;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;

import SFE.Compiler.Operators.AndOperator;
import SFE.Compiler.Operators.EqualOperator;
import SFE.Compiler.Operators.GreaterEqualOperator;
import SFE.Compiler.Operators.GreaterOperator;
import SFE.Compiler.Operators.LessEqualOperator;
import SFE.Compiler.Operators.LessOperator;
import SFE.Compiler.Operators.MinusOperator;
import SFE.Compiler.Operators.NotEqualOperator;
import SFE.Compiler.Operators.NotOperator;
import SFE.Compiler.Operators.Operator;
import SFE.Compiler.Operators.OrOperator;
import SFE.Compiler.Operators.PlusOperator;
import SFE.Compiler.Operators.ReinterpretCastOperator;
import SFE.Compiler.Operators.TimesOperator;
import SFE.Compiler.Operators.UnaryMinusOperator;
import SFE.Compiler.Operators.UnaryPlusOperator;
import SFE.Compiler.Operators.XOROperator;

/**
 * The SFECompiler class takes an input stream and checks if it is compatible
 * with the predefined language. It uses the class Tokenizer that gives tokens
 * and their values.
 */
public class SFECompiler {
  // ~ Instance fields
  // --------------------------------------------------------

  /*
   * Gives tokens and their values
   */
  private Tokenizer tokenizer;

  // ~ Constructors
  // -----------------------------------------------------------

  /**
   * Creates a tokenizer that parses the given stream.
   *
   * @param file
   *            a Reader object providing the input stream.
   */
  public SFECompiler(Reader file) {
    tokenizer = new Tokenizer(file);
  }

  // ~ Methods
  // ----------------------------------------------------------------

  /*
   * Advances one token
   *
   * @param error error message
   *
   * @throws IOException - if an I/O error occurs.
   *
   * @throws ParseException - if a parsing error occurs.
   */
  private void advance(String error) throws IOException, ParseException {
    if (!tokenizer.hasMoreTokens()) {
      throw new ParseException(error, tokenizer.lineNumber());
    }

    tokenizer.advance();
  }

  /**
   * Gets the next token from the stream and checks if it is from the expected
   * type.
   *
   * @param tokenType
   *            the expected type.
   * @param error
   *            string of the error message which will be send with
   *            ParseException.
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private void getNextToken(int tokenType, String error)
  throws ParseException, IOException {
    if (tokenizer.hasMoreTokens()) {
      tokenizer.advance();

      // checks if its the expected type
      if (tokenizer.tokenType() == tokenType) {
        return;
      }
    }

    throw new ParseException(error, tokenizer.lineNumber());
  }

  /**
   * Gets the next token from the stream, checks first if it is a keyword and
   * then if its the the expected keyword.
   *
   * @param keywordType
   *            the expected keyword type.
   * @param error
   *            string of the error message which will be send with
   *            ParseException.
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private void getKeyword(int keywordType, String error)
  throws ParseException, IOException {
    getNextToken(Tokenizer.KEYWORD, error);

    // checks if its the expected keyword
    if (tokenizer.keyword() != keywordType) {
      throw new ParseException(error, tokenizer.lineNumber());
    }
  }

  /**
   * Gets the next token from the stream, checks first if it is a symbol and
   * then if its the the expected symbol.
   *
   * @param symbol
   *            the expected symbol.
   * @param error
   *            string of the error message which will be send with
   *            ParseException.
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private void getSymbol(char symbol, String error) throws ParseException,
    IOException {
    getNextToken(Tokenizer.SYMBOL, error);

    if (tokenizer.symbol() != symbol) {
      throw new ParseException(error, tokenizer.lineNumber());
    }
  }

  /**
   * Compiles the const expression: (&lt;const&gt; | &lt;number&gt;) ((+ | -)
   * (&lt;const&gt; | &lt;number&gt;))*
   *
   * TODO: return a Number object which is either a BigInteger or a BigDecimal
   *
   * @return int the const value
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private ConstExpression compileConstValue() throws ParseException,
    IOException {
    //Array constant?
    if (tokenizer.tokenType() == Tokenizer.SYMBOL) {
      if (tokenizer.symbol() == '{') {
        advance("program not ended");

        //Only handle integer arrays
        ArrayConstant ac = new ArrayConstant(new IntType());

        while(true) {
          if (tokenizer.tokenType() == Tokenizer.SYMBOL) {
            if (tokenizer.symbol() == '}') {
              advance("program not ended");
              break;
            }
          }

          ConstExpression subValue = compileConstValue();
          ac.add(subValue);

          if (tokenizer.tokenType() == Tokenizer.SYMBOL) {
            if (tokenizer.symbol() == '}') {
              advance("program not ended");
              break;
            }
          }

          //Otherwise there must be a comma
          if ((tokenizer.tokenType() != Tokenizer.SYMBOL) || (tokenizer.symbol() != ',')) {
            throw new ParseException("Expected comma in array constant, got "
                                     + tokenizer.symbol(), tokenizer.lineNumber());
          }
          advance("program not ended");
        }

        return ac;
      }
    }

    FloatConstant val = FloatConstant.ZERO;
    FloatConstant tmp = FloatConstant.ZERO;
    FloatConstant neg1 = FloatConstant.NEG_ONE;
    char lastOperator = '+';

    do {
      switch (tokenizer.tokenType()) {
      case Tokenizer.IDENTIFIER:

        String id = tokenizer.getIdentifier();
        ConstExpression constExpr = Consts.fromName(id);
        tmp = FloatConstant.toFloatConstant(constExpr);

        advance("program not ended");

        break;

      case Tokenizer.INT_CONST:

        tmp = compileFraction();
        break;

      default:
        throw new ParseException("const or number is expected"
                                 + tokenizer.symbol(), tokenizer.lineNumber());
      }

      switch (lastOperator) {
      case '+':
        val = val.add(tmp);

        break;

      case '-':
        tmp = tmp.multiply(neg1);
        val = val.add(tmp);

        break;
      }

      if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
          || ((tokenizer.symbol() != '+') && (tokenizer.symbol() != '-'))) {
        break;
      }

      lastOperator = tokenizer.symbol();
      advance("program not ended");
    } while (true);

    return val;
  }

  private FloatConstant compileFraction() throws ParseException, IOException {
    int numerator = tokenizer.intVal();
    int denominator = 1;

    advance("program not ended");

    if (tokenizer.tokenType() == Tokenizer.IDENTIFIER) {
      if (tokenizer.getIdentifier().startsWith("x")) {
        if (!(numerator == 0)) {
          throw new RuntimeException("Error in hexadecimal notation");
        }
        String hex = "0"+tokenizer.getIdentifier();
        //Integer.decode doesn't handle 0xFXXXXXXX, because the result is negative.
        if (hex.toUpperCase().startsWith("0XF") && (hex.length() == 10)) {
          numerator = Integer.decode("0x"+hex.substring(3)) | 0xF0000000;
        } else {
          numerator = Integer.decode(hex);
        }
        advance("program not ended");
      }
    }

    if ((tokenizer.tokenType() == Tokenizer.SYMBOL)
        && (tokenizer.symbol() == '/')) {

      advance("program not ended");

      // Fraction notation

      if (tokenizer.tokenType() == Tokenizer.INT_CONST) {
        denominator = tokenizer.intVal();
      } else {
        throw new ParseException(
          "error parsing denominator of fraction"
          + tokenizer.symbol(), tokenizer.lineNumber());
      }

      advance("program not ended");
    }

    return FloatConstant.valueOf(numerator, denominator);
  }

  /**
   * Compiles the all program: program &lt;program-name&gt; { &lt;type
   * declarations&gt; &lt;function declarations&gt; }
   *
   * @return Program data structure that holds all the declarations and
   *         statements of the program
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  public void compileProgram(Program program) throws ParseException,
    IOException {
    // program
    getKeyword(Tokenizer.KW_PROGRAM, "program is expected");

    // <program-name>
    getNextToken(Tokenizer.IDENTIFIER,
                 "program name is expected after program");

    program.setName(tokenizer.getIdentifier());

    compileExternalFunctions(program);

    // {
    getSymbol('{',
              "{ is expected after program name " + tokenizer.getIdentifier());

    advance("program not ended");

    // <type declarations>
    compileTypeDeclarations(program);

    // <function declarations>
    compileFunctionDeclarations(program);

    // }
    if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
        || (tokenizer.symbol() != '}')) {
      throw new ParseException("} is expected at the end of program",
                               tokenizer.lineNumber());
    }
  }

  /**
   * Adds global functions to the program, which could otherwise not be easily
   * written in SFDL.
   *
   * These include: RANDOM_INT(numbits) - returns a random signed integer with
   * absolute value at most 2^numbits
   */
  private void compileExternalFunctions(Program program) {
    program.addFunction(new Function("FUNCTION_STATIC_RANDOM_INT",
    new FloatType(), false) {
      {
        addParameter("na", new IntType());
        // This is a hack.
        class CustomResult extends ConstExpression {
          public CustomResult() {
            super();
          }

          public Type getType() {
            throw new RuntimeException("Not implemented");
          }

          // Type: single bit
          public int size() {
            return 1;
          }

          public ConstExpression fieldEltAt(int i) {
            return this;
          }

          // Evaluate to const expression during uniqueVars
          public Expression changeReference(VariableLUT unique) {
            Random rgen = Function.staticFunctionRandom.get(Function.staticFunctionRandom.size() - 2);
            double rand = rgen.nextDouble();

            int na = IntConstant.toIntConstant(unique.getVar("FUNCTION_STATIC_RANDOM_INT$na")).toInt();
            if (na > 32) {
              throw new RuntimeException(
                "FUNCTION_STATIC_RANDOM_INT supports at most 32-bit random value generation");
            }
            // So na = 1 means uniform chance among -2, -1, 0, 1, 2.
            long result = (long) (((1L << (na + 1)) + 1) * rand);
            result -= 1L << na;

            IntConstant toRet = IntConstant.valueOf((int) result);

            ConstExpression randomVals = Consts.fromName(rgen.toString());
            ((ArrayConstant) randomVals).add(toRet);

            return toRet;
          }

          // Huh?
          public Vector getUnrefLvalInputs() {
            throw new RuntimeException();
          }
        }
        addStatement(new AssignmentStatement(
                       (LvalExpression) getFunctionResult(),
                       new UnaryOpExpression(new UnaryPlusOperator(),
                                             new CustomResult())));
      }
    });

    program.addFunction(new Function("GRAY_CODE",
    new IntType(), false) {
      {
        addParameter("i", new IntType());
        addParameter("j", new IntType());
        addParameter("n", new IntType());
        class CustomResult extends ConstExpression {
          public CustomResult() {
            super();
          }

          public Type getType() {
            throw new RuntimeException("Not implemented");
          }

          // Type: single bit
          public int size() {
            return 1;
          }

          public ConstExpression fieldEltAt(int i) {
            return this;
          }

          // Evaluate to const expression during uniqueVars
          public Expression changeReference(VariableLUT unique) {
            int i = IntConstant.toIntConstant(
                      unique.getVar("GRAY_CODE$i"))
                    .toInt();
            int j = IntConstant.toIntConstant(
                      unique.getVar("GRAY_CODE$j"))
                    .toInt();
            int n = IntConstant.toIntConstant(
                      unique.getVar("GRAY_CODE$n"))
                    .toInt();

            boolean result = GrayCode.getBit(i,j,n);

            //System.out.println(i+" "+j+" "+result);

            return BooleanConstant.valueOf(result);
          }

          // Huh?
          public Vector getUnrefLvalInputs() {
            throw new RuntimeException();
          }
        }
        addStatement(new AssignmentStatement(
                       (LvalExpression) getFunctionResult(),
                       new UnaryOpExpression(new UnaryPlusOperator(),
                                             new CustomResult())));
      }
    });

    /*
    program.addFunction(new Function("RANDOM_INT", new FloatType(), false) {
    	{
    		addParameter("na", new FloatType());
    		// This is a hack.
    		class CustomResult extends ConstExpression {
    			public CustomResult() {
    			}

    			public void updateRequiredBits(Object obj, Lvalue lvalue) {
    				// No information yet.
    			}

    			// Type: single bit
    			public int size() {
    				return 1;
    			}

    			public Expression bitAt(int i) {
    				return this;
    			}

    			// Evaluate to const expression during uniqueVars
    			public Expression changeReference(VariableLUT unique) {
    				int na = IntConstant
    						.toIntConstant(
    								((ConstExpression) unique
    										.getVar("RANDOM_INT$na")
    										.inline(null,
    												InliningConstraints.INLINE_LVAL_CONST)))
    						.value();
    				if (na > 30) {
    					throw new RuntimeException(
    							"RANDOM_INT supports at most 30-bit random value generation");
    				}
    				// So na = 1 means uniform chance among -2, -1, 0, 1, 2.
    				long result = (long) ((1L << (na + 1) + 1) * Math
    						.random());
    				result -= 1L << na;
    				return new IntConstant((int) result);
    			}

    			// Huh?
    			public Vector getLvalExpressionInputs() {
    				throw new RuntimeException();
    			}
    		}
    		addStatement(new AssignmentStatement(
    				(LvalExpression) getFunctionResult(),
    				new UnaryOpExpression(new UnaryPlusOperator(),
    						new CustomResult())));
    	}
    });

    program.addFunction(new Function("RANDOM_BOOLEAN", new BooleanType(),
    		false) {
    	{
    		// This is a hack.
    		addStatement(new AssignmentStatement(
    				(LvalExpression) getFunctionResult(),
    				new UnaryOpExpression(new UnaryPlusOperator(),
    						new ConstExpression() {
    							public void updateRequiredBits(Object obj,
    									Lvalue lvalue) {
    								lvalue.setBoolean();
    							}

    							public int size() {
    								return 1;
    							}

    							public Expression bitAt(int i) {
    								return this;
    							}

    							public Expression changeReference(
    									VariableLUT unique) {
    								return new BooleanConstant(Math
    										.random() < .5);
    							}

    							public Vector getLvalExpressionInputs() {
    								throw new RuntimeException();
    							}
    						})));
    	}
    });
    */
  }

  /**
   * Compiles the type declarations: ((const &lt;const-name&gt; =
   * &lt;const-value&gt;;) | (type &lt;type-name&gt; = &lt;data-type&gt;;))*
   *
   * @param Program
   *            data structure that holds all the declarations and statements
   *            of the program
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private void compileTypeDeclarations(Program program)
  throws ParseException, IOException {
    while (tokenizer.tokenType() == Tokenizer.KEYWORD) {
      // type or const
      switch (tokenizer.keyword()) {
      case Tokenizer.KW_TYPE:
        compileType();

        break;

      case Tokenizer.KW_CONST:
        compileConst();

        break;

      default:
        return;
      }

      // ;
      if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
          || (tokenizer.symbol() != ';')) {
        throw new ParseException("; is expected",
                                 tokenizer.lineNumber());
      }

      advance("program not ended");
    }
  }

  /**
   * Compiles the type declaration: &lt;type-name&gt; = &lt;data-type&gt;;
   *
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private void compileType() throws ParseException, IOException {
    // <type-name>
    getNextToken(Tokenizer.IDENTIFIER, "type name is expected after type");

    String typeName = tokenizer.getIdentifier();

    // =
    getSymbol('=', "= is expected after type name " + typeName);
    advance("type name is expected");

    // <data-type>
    Type.defineName(typeName, compileDataType());
  }

  /**
   * Compiles the const declaration: &lt;const-name&gt; = &lt;const-value&gt;;
   *
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private void compileConst() throws ParseException, IOException {
    // <const-name>
    getNextToken(Tokenizer.IDENTIFIER, "const name is expected after const");

    String constName = tokenizer.getIdentifier();

    // =
    getSymbol('=',
              "= is expected after const name " + tokenizer.getIdentifier());

    advance("program not ended");

    // <const-value>
    ConstExpression unnamedConst = compileConstValue();
    Consts.addConst(constName, unnamedConst, false);
  }

  /**
   * Compiles the data type: (Int&lt;bits&gt; | Boolean | &lt;type-name&gt;)
   * (&lt;array&gt;)*
   *
   * @return Type one of the data structure types
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private Type compileDataType() throws ParseException, IOException {
    Type newType;

    // (Int or Boolean) or <type-name>
    switch (tokenizer.tokenType()) {
    case Tokenizer.KEYWORD:
      newType = compileKnownType();

      break;

    case Tokenizer.IDENTIFIER:
      newType = Type.fromName(tokenizer.getIdentifier());

      if (newType == null) {
        throw new ParseException("Unknown type "
                                 + tokenizer.getIdentifier(), tokenizer.lineNumber());
      }

      advance("Program not ended");

      break;

    default:
      throw new ParseException("type name is expected",
                               tokenizer.lineNumber());
    }

    // <array>
    return compileArray(newType);
  }

  /**
   * Compiles the known types: Int, Boolean, StructType, Enum
   *
   * @return Type one of the data structure types
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private Type compileKnownType() throws ParseException, IOException {
    Type type;

    switch (tokenizer.keyword()) {
    case Tokenizer.KW_INT:
      type = new IntType();
      advance("Program not ended");

      if (tokenizer.tokenType() == Tokenizer.SYMBOL) {
        if (tokenizer.symbol() == '<') {
          // Optional size restrictions:
          advance("Program not ended");

          int na = IntConstant.toIntConstant(compileConstValue()).toInt();

          if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
              || (tokenizer.symbol() != '>')) {
            throw new ParseException(
              "> is expected at the end of Int size",
              tokenizer.lineNumber());
          }

          advance("Program not ended");
          type = new RestrictedSignedIntType(na);
        }
      }

      break;
    case Tokenizer.KW_BOOLEAN:
      type = new BooleanType();
      advance("Program not ended");
      break;

    case Tokenizer.KW_FLOAT:
      type = new FloatType();
      advance("Program not ended");

      if (tokenizer.tokenType() == Tokenizer.SYMBOL) {
        if (tokenizer.symbol() == '<') {
          // Optional size restrictions:
          advance("Program not ended");

          int na = IntConstant.toIntConstant(compileConstValue())
                   .toInt();

          // ','
          if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
              || (tokenizer.symbol() != ',')) {
            throw new ParseException(
              ", required to separate na and nb in a restricted na,nb float type",
              tokenizer.lineNumber());
          }
          advance("Program not ended");

          int nb = IntConstant.toIntConstant(compileConstValue())
                   .toInt();

          // '>'
          if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
              || (tokenizer.symbol() != '>')) {
            throw new ParseException(
              "> is expected after defining a restricted na,nb float type",
              tokenizer.lineNumber());
          }
          advance("Program not ended");

          type = new RestrictedFloatType(na, nb);
        }
      }

      break;

    case Tokenizer.KW_STRUCT:

      // {
      getSymbol('{', "{ is expected after StructType");
      type = new StructType();
      advance("Program not ended");

      // <fields>
      compileStructFields((StructType) type);

      // }
      if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
          || (tokenizer.symbol() != '}')) {
        throw new ParseException(
          "} is expected in the end of struct fields",
          tokenizer.lineNumber());
      }

      advance("Program not ended");

      break;

    case Tokenizer.KW_ENUM:

      // {
      getSymbol('{', "{ is expected after StructType");

      int index = 0;
      advance("Program not ended");

      while ((tokenizer.tokenType() != Tokenizer.SYMBOL)
             || !((tokenizer.symbol() == '}') && (index > 0))) {
        if (tokenizer.tokenType() != Tokenizer.IDENTIFIER) {
          throw new ParseException("identifier is expected in enum",
                                   tokenizer.lineNumber());
        }

        // <const-value>
        ConstExpression enumVal = IntConstant.valueOf(index);
        Consts.addConst(tokenizer.getIdentifier(), enumVal, false);
        index++;
        advance("Program not ended");

        if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
            || !((tokenizer.symbol() == '}') || (tokenizer.symbol() == ','))) {
          throw new ParseException("} or , is expected in enum",
                                   tokenizer.lineNumber());
        }

        if (tokenizer.symbol() == ',') {
          advance("Program not ended");
        }
      }

      // calculate number of bits, +1 for rounding
      type = new RestrictedSignedIntType((int) (PlusOperator.log2(index) + 1));
      advance("Program not ended");

      break;

    default:
      throw new ParseException(tokenizer.getKeyword()
                               + " is not a supported type", tokenizer.lineNumber());
    }

    return type;
  }

  /**
   * Compiles the array syntax: ('[' &lt;const-value&gt; ']')*
   *
   * @param base
   *            the base type of the array
   * @return Type array type
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private Type compileArray(Type base) throws ParseException, IOException {
    /* WRONG! SFDL was implemented so that array[3][5] is an array of 5 arrays of length 3.
     * We aren't in FORTRAN any more. array[3][5] should be an array of 3 arrays of length 5.
     *
    // [
    while ((tokenizer.tokenType() == Tokenizer.SYMBOL)
    		&& (tokenizer.symbol() == '[')) {
    	advance("program not ended");

    	int length = IntConstant.toIntConstant(compileConstValue()).value();

    	base = new ArrayType(base, length);

    	// ]
    	if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
    			|| (tokenizer.symbol() != ']')) {
    		throw new ParseException("] is expected",
    				tokenizer.lineNumber());
    	}

    	advance("program not ended");
    }

    // the next token after array
    return base;
    */

    Stack<Integer> lengths = new Stack();

    // [
    while ((tokenizer.tokenType() == Tokenizer.SYMBOL)
           && (tokenizer.symbol() == '[')) {
      advance("program not ended");

      lengths.push(IntConstant.toIntConstant(compileConstValue()).toInt());

      // ]
      if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
          || (tokenizer.symbol() != ']')) {
        throw new ParseException("] is expected",
                                 tokenizer.lineNumber());
      }

      advance("program not ended");
    }

    while(!lengths.isEmpty()) {
      base = new ArrayType(base, lengths.pop());
    }

    return base;
  }

  /**
   * Compiles the function declarations: ( function &lt;data-type&gt;
   * &lt;function-name&gt; ( &lt;arguments-list&gt; ) { &lt;var
   * declarations&gt; &lt;function body&gt; } )*
   *
   * @param Program
   *            data structure that holds all the declarations and statements
   *            of the program
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private void compileFunctionDeclarations(Program program)
  throws ParseException, IOException {
    // function
    while ((tokenizer.tokenType() == Tokenizer.KEYWORD)
           && (tokenizer.keyword() == Tokenizer.KW_FUNCTION)) {
      advance("program not ended");

      // <data-type>
      Type returnType = compileDataType();

      // <function-name>
      if (tokenizer.tokenType() != Tokenizer.IDENTIFIER) {
        throw new ParseException(
          "function name is expected after type",
          tokenizer.lineNumber());
      }

      String funcName = tokenizer.getIdentifier();
      boolean isOutput = false;
      if (funcName.equals("output")) {
        isOutput = true;
      }
      Function func = new Function(funcName, returnType, isOutput);

      // (
      getSymbol(
        '(',
        "( is expected after function name "
        + tokenizer.getIdentifier());

      advance("program not ended");

      // <arguments-list>
      compileArgumentsList(func);

      // The function prototype is now defined. Tell the program about the
      // prototype
      // The program may add statements to the body (external argument
      // inputAssignments, for instance)
      program.addFunction(func);

      // )
      if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
          || (tokenizer.symbol() != ')')) {
        throw new ParseException(
          ") is expected in the end of arguments",
          tokenizer.lineNumber());
      }

      // {
      getSymbol('{', "{ is expected after )");

      advance("program not ended");

      // <var declarations>
      compileVarDeclarations(func);

      // <function body>
      compileFunctionBody(func);

      // }
      if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
          || (tokenizer.symbol() != '}')) {
        throw new ParseException("} is expected at end of function",
                                 tokenizer.lineNumber());
      }

      advance("program not ended");
    }
  }

  /**
   * Compiles the arguments list: ( &lt;data-type&gt; &lt;argument-name&gt;
   * (,&lt;data-type&gt; &lt;argument-name&gt;)* )?
   *
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private void compileArgumentsList(Function func) throws ParseException,
    IOException {
    // empty arguments list
    if ((tokenizer.tokenType() == Tokenizer.SYMBOL)
        && (tokenizer.symbol() == ')')) {
      return;
    }

    do {
      // <data-type>
      Type type = compileDataType();

      // <argument-name>
      if (tokenizer.tokenType() != Tokenizer.IDENTIFIER) {
        throw new ParseException(
          "argument name is expected after type",
          tokenizer.lineNumber());
      }

      func.addParameter(tokenizer.getIdentifier(), type);

      advance("program not ended");

      // ,
      if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
          || (tokenizer.symbol() != ',')) {
        break;
      }

      advance("program not ended");
    } while (true);
  }

  /**
   * Compiles the fields in StructType: ( &lt;data-type&gt; &lt;field-name&gt;
   * (,&lt;data-type&gt; &lt;field-name&gt;)* )?
   *
   * @param StructType
   *            data structure that holds fields
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private void compileStructFields(StructType structType)
  throws ParseException, IOException {
    do {
      // <data-type>
      Type type = compileDataType();

      // <field-name>
      if (tokenizer.tokenType() != Tokenizer.IDENTIFIER) {
        throw new ParseException("field name is expected after type",
                                 tokenizer.lineNumber());
      }

      structType.addField(tokenizer.getIdentifier(), type);

      advance("program not ended");

      // ,
      if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
          || (tokenizer.symbol() != ',')) {
        break;
      }

      advance("program not ended");
    } while (true);
  }

  /**
   * Compiles the variables declarations: ( var &lt;variables-list&gt; )*
   *
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private void compileVarDeclarations(Function func) throws ParseException,
    IOException {
    // empty arguments list
    while ((tokenizer.tokenType() == Tokenizer.KEYWORD)
           && (tokenizer.keyword() == Tokenizer.KW_VAR)) {
      advance("program not ended");
      compileVariablesList(func);
    }
  }

  /**
   * Compiles the variables list: &lt;data-type&gt; &lt;variable-name&gt;
   * (,&lt;variable-name&gt;)* ;
   *
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private void compileVariablesList(Function func) throws ParseException,
    IOException {
    // <data-type>
    Type type = compileDataType();

    do {
      // <variable-name>
      if (tokenizer.tokenType() != Tokenizer.IDENTIFIER) {
        throw new ParseException(
          "variable name is expected after type",
          tokenizer.lineNumber());
      }

      func.addVar(tokenizer.getIdentifier(), type);

      advance("program not ended");

      if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
          || !((tokenizer.symbol() == ';') || (tokenizer.symbol() == ','))) {
        throw new ParseException(", or ; is expected",
                                 tokenizer.lineNumber());
      }

      if (tokenizer.symbol() == ';') {
        break;
      }

      advance("program not ended");
    } while (true);

    advance("program not ended");
  }

  /**
   * Compiles the function body: ( &lt;statement&gt; )*
   *
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private void compileFunctionBody(Function func) throws ParseException,
    IOException {
    while ((tokenizer.tokenType() != Tokenizer.SYMBOL)
           || (tokenizer.symbol() != '}'))
      func.addStatement(compileStatement());
  }

  /**
   * Compiles the statement: &lt;if-statement&gt; | &lt;for-statement&gt; |
   * &lt;var-name&gt; '=' &lt;expression&gt;; | &lt;block&gt;
   *
   * @return Statement data structure that holds statement
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private Statement compileStatement() throws ParseException, IOException {
    BlockStatement statement = null;

    switch (tokenizer.tokenType()) {
    case Tokenizer.IDENTIFIER:

      // Assignment statement
      Vector expressions = new Vector(1);
      Vector lengths = new Vector(1);

      // <var-name>
      statement = new BlockStatement();

      String varName = compileLHS(null, expressions, lengths, statement);

      // =
      if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
          || (tokenizer.symbol() != '=')) {
        throw new ParseException("= is expected after variable",
                                 tokenizer.lineNumber());
      }

      advance("program not ended");

      // <expression>
      Expression expr = compileExpression(false, statement);

      // must be an OperationExpression
      if (!(expr instanceof OperationExpression)) {
        expr = new UnaryOpExpression(new UnaryPlusOperator(), expr);
      }

      // expressions are array indexing expressions
      if (expressions.isEmpty()) {
        LvalExpression var = Function.getVar(varName);
        if (var == null) {
          throw new ParseException("No such variable "+varName,
                                   tokenizer.lineNumber());
        }
        statement.addStatement(new AssignmentStatement(var, (OperationExpression) expr));
      } else {
        statement.addStatement(arrayStatment(varName, expressions,
                                             lengths, (OperationExpression) expr));
      }

      // ;
      if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
          || (tokenizer.symbol() != ';')) {
        throw new ParseException("; is expected in end of command",
                                 tokenizer.lineNumber());
      }

      advance("program not ended");

      break;

    case Tokenizer.KEYWORD:

      // if or for
      switch (tokenizer.keyword()) {
      case Tokenizer.KW_IF:
        statement = compileIf();

        break;

      case Tokenizer.KW_FOR:
        statement = compileFor();

        break;

      default:
        throw new ParseException(tokenizer.getKeyword()
                                 + " is not a supported command", tokenizer.lineNumber());
      }

      break;

    case Tokenizer.SYMBOL:

      if (tokenizer.symbol() != '{') {
        throw new ParseException("unexpected symbol "
                                 + tokenizer.symbol(), tokenizer.lineNumber());
      }

      advance("program not ended");

      // <block>
      statement = new BlockStatement();

      while ((tokenizer.tokenType() != Tokenizer.SYMBOL)
             || (tokenizer.symbol() != '}')) {
        ((BlockStatement) statement).addStatement(compileStatement());
      }

      advance("program not ended");

      break;

    default:
      throw new ParseException("identifier or if or for is expected",
                               tokenizer.lineNumber());
    }

    return statement;
  }

  /**
   * Merges splitted array variable into statement(left side variable).
   *
   * @param varName
   *            variable name, $ is the splitted places
   * @param expressions
   *            splitted expressions
   * @param lengths
   *            limit of each index in the array
   * @param rhs
   *            right side expression
   * @return Statement data structure that holds statement
   */
  private Statement arrayStatment(String varName, Vector expressions,
                                  Vector lengths, OperationExpression rhs) {
    LvalExpression base = Function.getVar(varName);
    return new ArrayStatement(base, expressions, lengths, rhs);
    /*
     * int[] indexes = new int[lengths.size()];
     *
     * for (int i = 0; i < lengths.size(); i++) indexes[i] = 0;
     *
     * BlockStatement statement = new BlockStatement(); String[]
     * varNameSplited = varName.split("\\$");
     *
     * int lengthOfLast = ((Integer) lengths.elementAt(lengths.size() -
     * 1)).intValue();
     *
     * do { String str = new String();
     *
     * for (int i = 0; i < indexes.length; i++) str += (varNameSplited[i] +
     * indexes[i]);
     *
     * if (varNameSplited.length > indexes.length) { str +=
     * varNameSplited[varNameSplited.length - 1]; }
     *
     * //op is a boolean (0 or 1) BinaryOpExpression op = new
     * BinaryOpExpression(new EqualOperator(), new IntConstant(indexes[0]),
     * (Expression) expressions.elementAt(0));
     *
     * for (int i = 1; i < indexes.length; i++) { //op2 is a boolean (0 or
     * 1) BinaryOpExpression op2 = new BinaryOpExpression(new
     * EqualOperator(), new IntConstant(indexes[i]), (Expression)
     * expressions.elementAt(i)); op = new BinaryOpExpression(new
     * TimesOperator(), op, op2); }
     *
     * System.out.println(str+" "+Function.getVar(str));
     *
     * statement.addStatement(new IfStatement(op, new
     * AssignmentStatement(Function.getVar(str), rhs), null));
     *
     * //advance indices goes to the next, lexicographically ordered, index
     * string, or returns false if the array space is exhausted. } while
     * (advanceIndexes(indexes, lengths));
     *
     * return statement;
     */
  }

  /**
   * Advances the indexes of an array variable.
   *
   * @param indexes
   *            indeces of the array variable.
   * @param lengths
   *            limit of each index in the array
   * @return false if all indeces get to the limit.
   */
  private boolean advanceIndexes(int[] indexes, Vector lengths) {
    int i = indexes.length - 1;

    while (i >= 0) {
      indexes[i] = (indexes[i] + 1)
                   % ((Integer) lengths.elementAt(i)).intValue();

      if (indexes[i] == 0) {
        i--;
      } else {
        return true;
      }
    }

    return false;
  }

  /**
   * Compiles for statement: for '(' &lt;identifier&gt; '=' &lt;from-value&gt;
   * to &lt;to-value&gt; ')' &lt;statement&gt;
   *
   * @return BlockStatement data structure that holds for statement
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private BlockStatement compileFor() throws ParseException, IOException {
    // (
    getSymbol('(', "( is expected after for");

    // <identifier>
    getNextToken(Tokenizer.IDENTIFIER, "identifier is expected after (");

    // <var-name>
    Vector expressions = new Vector(1);
    Vector lengths = new Vector(1);
    BlockStatement block = new BlockStatement();
    String varName = compileLHS(null, expressions, lengths, block);

    // =
    if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
        || (tokenizer.symbol() != '=')) {
      throw new ParseException("= is expected after variable",
                               tokenizer.lineNumber());
    }

    advance("program not ended");

    // <from-value>
    Expression from = compileExpression(false, block);

    // to
    if ((tokenizer.tokenType() != Tokenizer.KEYWORD)
        || (tokenizer.keyword() != Tokenizer.KW_TO)) {
      throw new ParseException("to is expected", tokenizer.lineNumber());
    }

    advance("program not ended");

    // <to-value>
    Expression to = compileExpression(true, block);

    // )
    if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
        || (tokenizer.symbol() != ')')) {
      throw new ParseException(") is expected", tokenizer.lineNumber());
    }

    advance("program not ended");

    Statement forBlock = compileStatement();

    if (!expressions.isEmpty()) {
      throw new RuntimeException(
        "Unsupported operation: For loop iterators cannot be array members");
    }

    block.addStatement(new ForStatement(Function.getVar(varName), from, to, forBlock, ""+(labelIndex++), ""+(labelIndex++)));

    /*
     * //introduce the initial value of the looping variable if
     * (expressions.isEmpty()) { block.addStatement(new
     * AssignmentStatement(Function.getVar(varName), new
     * UnaryOpExpression(new UnaryPlusOperator(), new IntConstant(from))));
     * } else { block.addStatement(arrayStatment(varName, expressions,
     * lengths, new UnaryOpExpression(new UnaryPlusOperator(), new
     * IntConstant(from)))); }
     *
     * //duplicate the for loop until the looping variable leaves the bounds
     * while(true){ block.addStatement(forBlock);
     *
     * //Can we loop again? if (from + 1 <= to){ from++; if
     * (expressions.isEmpty()) { block.addStatement(new
     * AssignmentStatement(Function.getVar(varName), new
     * UnaryOpExpression(new UnaryPlusOperator(), new IntConstant(from))));
     * } else { block.addStatement(arrayStatment(varName, expressions,
     * lengths, new UnaryOpExpression(new UnaryPlusOperator(), new
     * IntConstant(from)))); }
     *
     * forBlock = forBlock.duplicate(); //We need a new copy for the next
     * iteration
     * ((Optimizable)forBlock).optimize(Optimization.DUPLICATED_IN_FUNCTION
     * ); } else { break; } }
     */

    return block;
  }

  /**
   * Compiles if statement: if '(' &lt;boolean-expr&gt; ')' &lt;statement&gt;
   * (else &lt;statement&gt;)?
   *
   * @return BlockStatement data structure that holds if statement
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private BlockStatement compileIf() throws ParseException, IOException {
    // (
    getSymbol('(', "( is expected after if");

    BlockStatement block = new BlockStatement();

    // we don't need advance, the expression need to take (
    // <boolean-expr>
    Expression condition = compileExpression(false, block);

    Statement thenBlock = compileStatement();
    Statement elseBlock;

    // else
    if ((tokenizer.tokenType() == Tokenizer.KEYWORD)
        && (tokenizer.keyword() == Tokenizer.KW_ELSE)) {
      advance("program not ended");

      elseBlock = compileStatement();
    } else {
      elseBlock = new BlockStatement();
    }

    block.addStatement(new IfStatement(condition, thenBlock, elseBlock, ""+(labelIndex++)));

    return block;
  }

  /**
   * Compiles left value: &lt;identifier&gt; ( '[' &lt;expression&gt; ']' |
   * '.'&lt;var-name&gt;)?
   *
   * @param varName
   *            variable name
   * @param block
   *            holds a block of statements
   * @return LvalExpression data structure of left value or null if identifier
   *         does not exists.
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private Expression compileLvalExpression(String varName,
      Vector<Expression> expressions,// Vector<Integer> lengths,
      BlockStatement block) throws ParseException, IOException {
    String fieldName = tokenizer.getIdentifier();

    if (varName == null) {
      varName = fieldName;
    } else {
      varName += ('.' + fieldName);
    }

    advance("program not ended");

    if (tokenizer.tokenType() == Tokenizer.SYMBOL) {
      switch (tokenizer.symbol()) {
      case '(':

        /*
         * we have already passed a struct or an array - no '(' should
         * be here!!!
         */
        if (varName.equals(fieldName)) {
          return null;
        }

        throw new ParseException("Unexpected '(' sign",
                                 tokenizer.lineNumber());

      case '[':

        while ((tokenizer.tokenType() == Tokenizer.SYMBOL)
               && (tokenizer.symbol() == '[')) {
          advance("program not ended");

          // <expression>
          Expression index = compileExpression(false, block);

          // ]
          if ((tokenizer.tokenType() == Tokenizer.SYMBOL)
              && (tokenizer.symbol() == ']')) {
            advance("program not ended");
          } else {
            throw new ParseException("] is expected",
                                     tokenizer.lineNumber());
          }
          /*
          int len = ((ArrayType) Function.getVar(varName).getType())
          		.getLength();
          lengths.add(len);
          */
          expressions.add(index);
          varName += "[$]";
        }

        if ((tokenizer.tokenType() == Tokenizer.SYMBOL)
            && (tokenizer.symbol() == '.')) {
          advance("program not ended");

          return compileLvalExpression(varName, expressions,// lengths,
                                       block);
        }

        break;

      case '.':

        // <identifier>
        getNextToken(Tokenizer.IDENTIFIER,
                     " identifier is expected after .");

        // <var-name>
        return compileLvalExpression(varName, expressions,// lengths,
                                     block);
      }
    }

    //varName could be const..
    Expression base = Consts.fromName(varName);
    if (base == null) {
      //Or a variable
      base = Function.getVar(varName);
    }
    if (base == null) {
      throw new ParseException("No such variable: "+varName, tokenizer.lineNumber());
    }
    if (expressions.isEmpty()) {
      //Then base does not have any array subexpressions
      return base;
    } else {
      // We have some array indices which must be resolved. Unfortunately,
      // this can't be done during compileProgram(),
      // but it can be done during uniqueVars(), when control flow is
      // resolved and variables can be inlined.

      /*
       * Function.addVar("tmp" + labelIndex, base.getType());
       * LvalExpression lval = Function.getVar("tmp" + labelIndex);
       * labelIndex++;
       *
       * block.addStatement(new AssignmentStatement(lval, new
       * ArrayExpression(varName, expressions, lengths, base.getType())));
       */

      return new ArrayAccessExpression(base, expressions, /*lengths,*/ base.getType());
    }
  }

  /**
   * Compiles left expression: &lt;identifier&gt; ( '[' &lt;expression&gt; ']'
   * | '.'&lt;var-name&gt;)?
   *
   * @param varName
   * @param expressions
   *            splited array expressions
   * @param length
   *            limit of each array index
   * @param block
   *            holds statements
   * @return String variable name splitted with $ instead of []
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private String compileLHS(String varName, Vector expressions,
                            Vector lengths, BlockStatement block) throws ParseException,
    IOException {
    String fieldName = tokenizer.getIdentifier();

    if (varName == null) {
      varName = fieldName;
    } else {
      varName += fieldName;
    }

    advance("program not ended");

    if (tokenizer.tokenType() == Tokenizer.SYMBOL) {
      switch (tokenizer.symbol()) {
      case '[':

        while ((tokenizer.tokenType() == Tokenizer.SYMBOL)
               && (tokenizer.symbol() == '[')) {
          advance("program not ended");

          // <expression>
          expressions.add(compileExpression(false, block));

          lengths.add(new Integer(((ArrayType) (Function
                                                .getVar(varName).getType())).getLength()));

          // ]
          if ((tokenizer.tokenType() == Tokenizer.SYMBOL)
              && (tokenizer.symbol() == ']')) {
            advance("program not ended");
          } else {
            throw new ParseException("] is expected",
                                     tokenizer.lineNumber());
          }

          varName += "[$]";
        }

        if ((tokenizer.tokenType() == Tokenizer.SYMBOL)
            && (tokenizer.symbol() == '.')) {
          advance("program not ended");

          return compileLHS(varName+".", expressions, lengths,
                            block);
        }

        break;

      case '.':

        // <identifier>
        getNextToken(Tokenizer.IDENTIFIER,
                     " identifier is expected after .");

        // <var-name>
        return compileLHS(varName+".", expressions, lengths, block);
      }
    }

    return varName;
  }

  /**
   * Compiles the expression: ('~' | '-')? &lt;term&gt; (&lt;operator&gt;
   * &lt;expression&gt;)?
   *
   * @param isArgument
   *            if it is an argument
   * @param block
   *            holds statements
   * @return Expression data structure of expression
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private Expression compileExpression(boolean isArgument,
                                       BlockStatement block) throws ParseException, IOException {
    Expression exp = null;

    try {
      Stack operators = new Stack();
      Stack operands = new Stack();

      do {
        while (tokenizer.tokenType() == Tokenizer.SYMBOL) {
          switch (tokenizer.symbol()) {
          case '!':
            insertNewOperator(operators, operands,
                              new NotOperator());

            break;

          case '-':
            insertNewOperator(operators, operands,
                              new UnaryMinusOperator());

            break;

          case '+':
            insertNewOperator(operators, operands,
                              new UnaryPlusOperator());

            break;

          case '(':
            operators.push(null);

            break;

          default:
            throw new ParseException(
              "! or - or ( or + is expected",
              tokenizer.lineNumber());
          }

          advance("program not ended");
        }

        // <term>
        operands.push(compileTerm(block));

        /*
        // <operator>
        if (tokenizer.tokenType() != Tokenizer.SYMBOL) {
        	throw new ParseException(
        			"Must be ) or ; or , or operator after term",
        			tokenizer.lineNumber());
        }
        */

        // flag for dealing with expression: ((...))
        boolean stackUpdated = false;

        while (tokenizer.symbol() == ')') {
          // there isn't ( for the last ) in argumnet list
          if (isArgument && operators.empty()) {
            break;
          }

          if ((operators.peek() == null) && (operands.empty())
              && !stackUpdated) {
            throw new ParseException("Empty expression ()",
                                     tokenizer.lineNumber());
          }

          stackUpdated = true;

          // until (
          while (!operators.empty() && operators.peek() != null)
            updateStacks(operators, operands);

          // pop (
          if (!operators.empty() && operators.peek() == null) {
            operators.pop();
            advance("program not ended");
          }

          if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
              || (tokenizer.symbol() == '{')) {
            break;
          }
        }

        if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
            || (tokenizer.symbol() == ';')
            || (tokenizer.symbol() == '{')
            || (tokenizer.symbol() == ']')
            || (isArgument && ((tokenizer.symbol() == ',') || (tokenizer.symbol() == ')')))) {
          break;
        }

        compileOperator(operators, operands);
      } while (true);

      while (!operators.empty())
        updateStacks(operators, operands);

      exp = (Expression) operands.pop();

      if (!operands.empty()) {
        throw new ParseException(
          "Error in expression, not enougth operators",
          tokenizer.lineNumber());
      }
    } catch (EmptyStackException ese) {
      throw new ParseException("Error in expression",
                               tokenizer.lineNumber());
    }

    return exp;
  }

  /**
   * Compiles the term: &lt;var-name&gt; | &lt;number&gt; | &lt;identifier&gt;
   * '(' (&lt;expression&gt; (',' &lt;expression&gt;)*)? ')'
   *
   * @param block
   *            holds statements
   * @return Expression data structure of expression
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private Expression compileTerm(BlockStatement block) throws ParseException,
    IOException {
    Expression expression = null;

    switch (tokenizer.tokenType()) {
    case Tokenizer.IDENTIFIER:

      String identifier = tokenizer.getIdentifier();

      /* in case the identifier is a defined const
      expression = Consts.fromName(identifier);

      if (expression != null) {
      	advance("program not ended");

      	break; // break the switch
      }
      */

      /* in case expression is a variable (lvalue) */
      expression = compileLvalExpression(null, new Vector(1),/* new Vector(1),*/ block);

      if (expression != null) {
        break; // break the switch
      }

      if ((tokenizer.tokenType() != Tokenizer.SYMBOL)
          || (tokenizer.symbol() != '(')) {
        throw new ParseException(
          "error in term: no const, var, function",
          tokenizer.lineNumber());
      }

      advance("program not ended");

      //Meta function call?
      if (identifier.equals("reinterpret_cast")) {
        Type targetType = compileDataType();
        if (!((tokenizer.tokenType() == Tokenizer.SYMBOL) && (tokenizer.symbol() == ','))) {
          throw new ParseException("Expected , after type in reinterpret_cast",
                                   tokenizer.lineNumber());
        }
        advance("program not ended");
        Expression toCast = compileExpression(true, block);

        expression = new UnaryOpExpression(
          new ReinterpretCastOperator(targetType),
          toCast);

        if (!((tokenizer.tokenType() == Tokenizer.SYMBOL) && (tokenizer.symbol() == ')'))) {
          throw new ParseException("Argument list must end with a )",
                                   tokenizer.lineNumber());
        }
        advance("program not ended");
      } else {
        // Standard function call
        Function calledFunc = Program.functionFromName(identifier);
        if (calledFunc == null) {
          throw new ParseException("No such function \"" + identifier
                                   + "\"", tokenizer.lineNumber());
        }
        if (calledFunc == Function.currentFunction) { // currentFunction is
          // the one being
          // compiled.
          throw new ParseException("Recursion is not implemented.",
                                   tokenizer.lineNumber());
        }

        int i = 0;

        // adding arguments assigning statements
        for (Enumeration e = calledFunc.getArguments().elements(); e.hasMoreElements()
             && !((tokenizer.tokenType() == Tokenizer.SYMBOL) && (tokenizer.symbol() == ')')); i++) {
          Object nexE = e.nextElement();

          block.addStatement(new AssignmentStatement((LvalExpression) nexE,
                             new UnaryOpExpression(new UnaryPlusOperator(),
                                 compileExpression(true, block))));

          if ((tokenizer.tokenType() == Tokenizer.SYMBOL)
              && (tokenizer.symbol() == ',')) {
            advance("program not ended");
          }
        }
        if (!((tokenizer.tokenType() == Tokenizer.SYMBOL) && (tokenizer.symbol() == ')'))) {
          throw new ParseException("Argument list must end with a )",
                                   tokenizer.lineNumber());
        }
        advance("program not ended");

        if (i != calledFunc.getArguments().size()) {
          throw new ParseException("Number of argument not match when calling "+calledFunc.getName(),
                                   tokenizer.lineNumber());
        }

        // add in a copy of the function body.
        block.addStatement(calledFunc.getBody().duplicate());

        //Give the return value of the function a unique id

        String name = "tmp"+(labelIndex++);
        Function.currentFunction.addVar(name, calledFunc.getFunctionResult().getType());
        LvalExpression lval = Function.getVar(name);

        block.addStatement(new AssignmentStatement(
                             lval,
                             new UnaryOpExpression(new UnaryPlusOperator(), calledFunc.getFunctionResult())
                           ));
        expression = lval;
      }

      break;

    case Tokenizer.INT_CONST:
      expression = compileFraction();

      break;

    case Tokenizer.KEYWORD:

      switch (tokenizer.keyword()) {
      case Tokenizer.KW_TRUE:
        expression = new BooleanConstant(true);

        break;

      case Tokenizer.KW_FALSE:
        expression = new BooleanConstant(false);

        break;

      default:
        throw new ParseException("Unexpected keyword "
                                 + tokenizer.getKeyword(), tokenizer.lineNumber());
      }

      advance("program not ended");

      break;

    default:
      throw new ParseException("number or identifier is expected",
                               tokenizer.lineNumber());
    }

    return expression;
  }

  /**
   * Pops operator and operand/s and push back a new operand which is the
   * expression of them.
   *
   * @param operators
   *            stack of operators
   * @param operands
   *            stack of operands
   * @throws EmptyStackException
   *             if trying to pop from empty stack.
   */
  private void updateStacks(Stack operators, Stack operands)
  throws EmptyStackException {
    Operator operator = (Operator) operators.pop();
    Expression right = (Expression) operands.pop();

    // if unary operator
    if (operator instanceof UnaryOperator) {
      operands.push(new UnaryOpExpression(operator, right));
    } else { // pop another operand for binary operator
      Expression left = (Expression) operands.pop();
      operands.push(new BinaryOpExpression(operator, left, right));
    }
  }

  /**
   * Inserts a new operator to operators stack. If there are operators with
   * higher priority call updateStacks with them.
   *
   * @param operators
   *            stack of operators
   * @param operands
   *            stack of operands
   * @param operator
   *            operator
   * @throws EmptyStackException
   *             if trying to pop from empty stack.
   */
  private void insertNewOperator(Stack operators, Stack operands,
                                 Operator operator) throws EmptyStackException {
    while (!operators.empty()
           && (operators.peek() != null)
           && (operator.priority() <= ((Operator) operators.peek())
               .priority()))
      updateStacks(operators, operands);

    operators.push(operator);
  }

  /**
   * Compiles the binary operator.
   *
   * @param operators
   *            stack of operators
   * @param operands
   *            stack of operands
   * @throws IOException
   *             - if an I/O error occurs.
   * @throws ParseException
   *             - if a parsing error occurs.
   */
  private void compileOperator(Stack operators, Stack operands)
  throws ParseException, IOException {
    //boolean getNextToken = true;

    switch (tokenizer.symbol()) {
    case '+':
      advance("program not ended");
      insertNewOperator(operators, operands, new PlusOperator());

      break;

    case '-':
      advance("program not ended");
      insertNewOperator(operators, operands, new MinusOperator());

      break;

    case '*':
      advance("program not ended");
      insertNewOperator(operators, operands, new TimesOperator());

      break;

    case '|':
      advance("program not ended");
      if ((tokenizer.tokenType() == Tokenizer.SYMBOL)
          && (tokenizer.symbol() == '|')) {
        throw new ParseException("Error: || operation (shortcircuiting boolean or) is not supported. Use | instead.",
                                 tokenizer.lineNumber());
      }
      insertNewOperator(operators, operands, new OrOperator());

      break;

    case '&':
      advance("program not ended");
      if ((tokenizer.tokenType() == Tokenizer.SYMBOL)
          && (tokenizer.symbol() == '&')) {
        throw new ParseException("Error: && operation (shortcircuiting boolean and) is not supported. Use & instead.",
                                 tokenizer.lineNumber());
      }
      insertNewOperator(operators, operands, new AndOperator());

      break;

    case '^':
      advance("program not ended");
      insertNewOperator(operators, operands, new XOROperator());

      break;

    case '<':
      advance("program not ended");

      if ((tokenizer.tokenType() == Tokenizer.SYMBOL)
          && (tokenizer.symbol() == '=')) {
        advance("program not ended");
        insertNewOperator(operators, operands, new LessEqualOperator());
      } else {
        insertNewOperator(operators, operands, new LessOperator());
      }

      break;

    case '>':
      advance("program not ended");

      if ((tokenizer.tokenType() == Tokenizer.SYMBOL)
          && (tokenizer.symbol() == '=')) {
        advance("program not ended");
        insertNewOperator(operators, operands, new GreaterEqualOperator());
      } else {
        insertNewOperator(operators, operands, new GreaterOperator());
      }

      break;

    case '=':
      advance("program not ended");

      if ((tokenizer.tokenType() == Tokenizer.SYMBOL)
          && (tokenizer.symbol() == '=')) {
        advance("program not ended");
        insertNewOperator(operators, operands, new EqualOperator());
      } else {
        throw new ParseException("There should be = after =",
                                 tokenizer.lineNumber());
      }

      break;

    case '!':
      advance("program not ended");

      if ((tokenizer.tokenType() == Tokenizer.SYMBOL)
          && (tokenizer.symbol() == '=')) {
        advance("program not ended");
        insertNewOperator(operators, operands, new NotEqualOperator());
      } else {
        throw new ParseException("There should be = after !",
                                 tokenizer.lineNumber());
      }

      break;

    default:
      throw new ParseException("Must be operator or ; or , or { or ]",
                               tokenizer.lineNumber());
    }
  }


  /*
   * label for temporary variables
   */
  static int labelIndex = 0;
}
