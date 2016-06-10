// Function.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Stack;
import java.util.Vector;

import SFE.Compiler.Operators.UnaryPlusOperator;


/**
 * A class for representing a function that can be defined
 * in the program.
 */
public class Function implements Optimizable, SLPTReduction {
  //~ Instance fields --------------------------------------------------------

  //data members

  /*
   * Holds the name of the function.
   */
  private final String name;

  /*
   * Holds whether this function is the output function (main).
   * At most one function can have this property.
   */
  private boolean isOutput;

  /*
   * Holds the returned type of the function.
   */
  private Type returnType;

  /*
   * Holds the body of this function (statements to be carried out).
   */
  private FunctionBody body;

  /*
   * Used to seed static function random variables
   */
  public final long randomSeed;

  /*
   * local parameters used only when a call is made
   */
  public Vector parameters;

  /*
   * holds the LvalExpression returned from this functin.
   *
   */
  private LvalExpression functionResult;

  //~ Constructors -----------------------------------------------------------

  /**
   * Sets the function's name and its returntype, but does not generate functionResult
   * or modify the variable table or static tables in any way.
   */
  public Function(String name, Type returnType) {
    this.name           = name;
    this.returnType     = returnType;
    this.body           = new FunctionBody(this);
    this.parameters     = new Vector();
    this.randomSeed = functionSeedGenerator.nextLong();
  }

  /**
   * Constructs a new Function from a given name, returned type. Modifies the variable table.
   */
  public Function(String name, Type returnType, boolean isOutput) {
    this(name, returnType);
    currentFunction     = this;

    this.isOutput = isOutput;

    //Add the output variable of this function
    vars.addVar( /*currentFunction.getName()*/
      name + "$" + name, returnType,
      /*isParameter=*/ false, /*isOutput=*/
      isOutput);

    functionResult = vars.getVar(name + "$" + name);

    //Zero out return value at start of function call (hide the fact that functions have global scopes)
    body.addStatement(new AssignmentStatement(functionResult,
                      new UnaryOpExpression(
                        new UnaryPlusOperator(),
                        IntConstant.ZERO
                      )));
  }

  //~ Methods ----------------------------------------------------------------

  public boolean isOutput() {
    return isOutput;
  }
  /**
   * Returns all variables and their references in all scopes.
   * @return UniqueVariables all variables and their references in all scopes.
   */
  public static VariableLUT getVars() {
    return vars;
  }

  /**
  * Adds a parameter to this function.
  * Note that the function adds the LvalExpression for this parameter.
  * @param name the name of the new parameter
  * @param type the type of the new parameter
  */
  public void addParameter(String name, Type type) {
    vars.addVar(this.name + "$" + name, type,
                /*isParameter=*/ true, /*isOutput=*/
                false);

    parameters.add(vars.getVar(this.name + "$" + name));
  }

  /**
   * Adds a local variable that was defined to this function.
   * Note that the function adds the LvalExpression for this parameter.
   * @param name the name of the new local variable
   * @param type the type of the new local variable
   */
  public void addVar(String name, Type type) {
    String varName = getName() + "$" + name;
    vars.addVar(varName, type,
                /*isParameter=*/ false, /*isOutput=*/
                false);

    LvalExpression var = vars.getVar(varName);

    //Zero out variable at start of function call (hide the fact that functions have global scopes)
    body.addStatement(new AssignmentStatement(var,
                      new UnaryOpExpression(
                        new UnaryPlusOperator(),
                        IntConstant.ZERO
                      )));
  }

  /**
  * Adds a local variable that was defined to this functioni, from a
  * given LvalExpression.
  * @param exp the given expression.
   * @return
  */
  public static void addVar(LvalExpression exp) {
    vars.addVar(exp.getName(), exp.getDeclaredType(), //copy the declared type
                /*isParameter=*/ false, /*isOutput=*/
                exp.isOutput());

    // if old lhs is output pin then set it to be non output
    //exp.notOutput();
  }

  /**
       * Adds a temporary local varivable as single bit LvalExpression
       * from a given varname and type and returns the LvalExpression that
       * hold the whole (original) variable. 
       * 
       * NOTE: this method differs from simply calling vars.add() because all bits of the
       * variable are added. This is useful when generating temp local variables after the
       * toSLPT phase has been passed.
       * 
       * @param name the name ND the temp. variable.
       * @param type the type of the temp. variable.
       * @return the LvalExpression that represents the variable.
       */
  public static LvalExpression addTempLocalVar(String name, Type type) {
    return vars.addVar(name, type, false, false);

    /*
    Lvalue lvalue = new VarLvalue(new Variable(name, type), false);
    //vars.add(lvalue, false); //Add un-bit split version, so that we don't add this guy twice.
    //vars.addVar(name, type, false, false);
    LvalExpression lvalExp = new LvalExpression(lvalue);
    //lvalExp = vars.getVar(name);
    //Lvalue lvalue = lvalExp.getLvalue();

    // add the lvalue's bits
    for (int j = 0; j < lvalue.size(); j++) {
      vars.add(new BitLvalue(lvalue, j), false);
    }

    return lvalExp;
    */
  }

  /**
       * Returns a string representation of the object.
        * @return a string representation of the object.
       */
  public String toString() {
    String str = new String("function " + returnType + " " + name + "\n");

    for (Statement s : body.getStatements())
      str += (s).toString();

    return str;
  }

  /**
       * Return the name of the function.
       * @return the name of the function.
       */
  public String getName() {
    return name;
  }

  /**
         * Adds a statement to this function.
         * @param statement the new statement.
         */
  public void addStatement(Statement statement) {
    body.addStatement(statement);
  }

  /**
   * Adds a statement to this function.
   * @param statement the new statement.
   */
  public void addStatements(Vector statements) {
    body.addStatements(statements);
  }

  /**
   * Returns the LvalExpression from a given
   * parameter or a local variable name. If the given
   * name does not exists, the returned value will be null.
   * @param name the function, parameter or a local variable name.
   * @return the LvalExpression representing the given name or
   * null if the given name does not exists.
   */
  public static LvalExpression getVar(String name) {
    // in case the name represents the name of a localvar
    // parameter or the function name OR
    // the name does not exists which is this case the return value
    // will be null
    return (LvalExpression) (vars.getVar(currentFunction.getName() + "$" +
                                         name));
  }

  /**
   * this method is used to get the last referance existing
   * (unique var)
   */
  public static LvalExpression getVar(LvalExpression lval) {
    // in case the name represents the name of a localvara
    // parameter or the function name OR
    // the name does not exists which is this case the return value
    // will be null
    return (LvalExpression) (vars.getVar(lval.getName()));
  }

  /**
       * Transfroms complex statements in the function into arithmetic
       * statements.
       * @param obj not needed (null).
       * @return null.
       */
  public BlockStatement toSLPTCircuit(Object obj) {
    currentFunction = this;

    FunctionBody oldBody = body;
    body = new FunctionBody(this);

    for (Statement s : oldBody.getStatements()) {
      body.addStatement(((SLPTReduction) s).toSLPTCircuit(null));
    }

    return null;
  }

  /*
     * Adds the input statements into the output function.
     */
  public void addInputStatements() {
    Vector parameters = vars.getParameters();

    // run over all the parameters LvaluesExpressions
    BlockStatement inputs = new BlockStatement();
    for (int i = 0; i < parameters.size(); i++) {
      LvalExpression parameterLvalExp =
        (LvalExpression) (parameters.elementAt(i));
      String parameterName = parameterLvalExp.getName();

      if (! parameterName.startsWith(name + "$")) {
        continue;
      }

      // insert input statement at the start of the function
      InputStatement is = new InputStatement(parameterLvalExp);
      inputs.addStatement(is);
    }
    body.getStatements().add(0, inputs);
    //for
  }

  // end method

  /**
   * Prints this Function into the circuit.
   *  @param circuit the circuit output file.
  public void toCircuit(PrintWriter circuit) {
  	currentFunction = this;

  	body.toCircuit(circuit);
  }
   */

  /**
   * Optimizes this function - phase I.
   * Run phase I on all the functions statements.
   */
  public void optimize(Optimization job) {
    currentFunction = this;

    //As long as all children implement the optimization, this function does
    for (Statement s : body.getStatements())
      ((Optimizable) (s)).optimize(job);
  }

  /**
   * creates a list of the needed statements in this functions and removes
   * all unneeded statements according to this list.
   * @param newBody newBody is always null (needed for other classes).
   */
  public void blockOptimize(BlockOptimization job, List newBody) {
    currentFunction = this;

    switch(job) {
    case DEADCODE_ELIMINATE:
      buildUsedStatementsHash();

      // Build new body which contains only used elements
      FunctionBody oldBody = body;
      body = new FunctionBody(this);

      for (Statement s1 : oldBody.getStatements()) {
        Optimizable s = ((Optimizable) (s1));
        s.blockOptimize(BlockOptimization.DEADCODE_ELIMINATE, body.getStatements());
      }
      break;
    default:
      //It's dangerous to run an optimization which not all parts of the system have implemented.
      //Catch this case.
      throw new RuntimeException("Optimization not implemented: "+job);
    }
  }

  /**
   * create the list of all the needed statements to calculate the circuit.
   * this list is used in the optimization.
   */
  public void buildUsedStatementsHash() {
    for (int i = body.getStatements().size() - 1; i >= 0; i--)
      // Block/Assignment/Input Statement
      ((Optimizable) (body.getStatements().get(i))).buildUsedStatementsHash();
  }

  /**
   * Unique vars transformations.
  public void uniqueVars() {
  	currentFunction = this;

  	for (int i = 0; i < body.getStatements().size(); i++)
  		body.getStatements().set(i,body.getStatements().get(i).uniqueVars());
  }
   */
  
  /**
   * Called when begining a new scope (for example: if, for)
   */
  public static void pushScope() {
    vars.pushScope();
  }

  /**
   * Called when ending the current scope
   * @return HashMap that holds the variables of the scope
   * and their references
   */
  public static Map popScope() {
    return vars.popScope();
  }

  /**
       * returns the arguments of this function.
       * @return the arguments of this function.
       */
  public Vector getArguments() {
    return parameters;
  }

  /**
       * returns the body of this function
       * @return the body of this function
       */
  public FunctionBody getBody() {
    return body;
  }


  //~ Static fields/initializers ---------------------------------------------

  public LvalExpression getFunctionResult() {
    return functionResult;
  }

  /*
   * Holds all variables and their references in all scopes.
   */
  private static VariableLUT vars = new VariableLUT();

  /*
   * Holds the current function that curetly being parsed.
   */
  public static Function currentFunction;

  /*
   * Used during toCircuit to provide function-level static random values
   */
  public static Stack<Random> staticFunctionRandom = new Stack();

  /*
   * Used to generate random seeds for each function, which are used to fill in FUNCTION_STATIC_RANDOM_INT queries.
   */
  public static Random functionSeedGenerator = new Random(System.nanoTime());

  public void setOutput(boolean b) {
    isOutput = b;
  }
}
