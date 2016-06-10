// Optimizer.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.HashMap;


/**
 * Optimizer holds all the data structures needed for the optimization process.
 */
public class Optimizer {
  //~ Methods ----------------------------------------------------------------

  public static void initOptimizer() {
    memo.clear();
    usedStatements.clear();
  }

  /**
   * Associates the given holdinglval with a particular expression
   *
   * @param gate the gate (OperationExpression) to be added.
   * @param holdingLvalExpression the lValexpression that hold the result of the gate.
   */
  public static void addAssignment(Expression expr,
                                   LvalExpression holdingLvalExpression) {
    //removeLvalFor(expr);
    memo.put(expr.toString(),holdingLvalExpression);
  }

  /**
   * Returns true if there is an lvalue which holds an expression expr
   * @param expr the gate (OperationExpression) whose presence in the optimizer is to be tested.
   * @return true if optimizer contains a mapping for the specified gate.
  public static boolean containsLvalFor(Expression expr) {
  	return memo.containsKey(expr.toString());
  }
   */
  public static LvalExpression getLvalFor(Expression expr) {
    return memo.get(expr.toString());
  }
  public static void removeLvalFor(Expression expr) {
    LvalExpression removed = memo.remove(expr.toString()); //The memo counts as a reference
    /*
    if (removed == null){
    	throw new RuntimeException("Assertion error: attempt to remove lval-expression mapping failed");
    }
    */
  }

  /**
   * Adds an AssignmentStatement to the usage data structure.
   * @param as the AssignmentStatement to be added.
   */
  public static void putUsedStatement(Statement s) {
    throw new RuntimeException("Not yet implemented");
    /*
    usedStatements.put(s, null);
    if (s instanceof AssignmentStatement){
    	System.out.println("Used statement: "+((AssignmentStatement)s).getOutputLine());
    }
    if (s instanceof InputStatement){
    	System.out.println("Used input statement: "+((InputStatement)s).getOutputLine());
    }
    */
  }


  //~ Static fields/initializers ---------------------------------------------

  /**
   * This data structures holds memo-ized expressions and their lval assignments.
   * Making use of this data structure allows us to replace the same expression, every time it
   * occurs after the first time in the code, with an lvalue.
   */
  private static HashMap<String, LvalExpression> memo = new HashMap();

  /*
   * This data structure holds all the statements and their
   * expressions (which expression is being assigned and using
   * which expressions).
   * At the second phase of the optimization algorithm, the optimizer
   * will remove all the statements that are being used in order to
   * compute the programs output. Afterwards the statement that will be left
   * in usage table can be removed from the program.
   */
  private static HashMap<Statement, Object> usedStatements = new HashMap();

  /**
   * A global symbol referring to what target to optimize for.
   */
  public static Target optimizationTarget;
  private static boolean isFirstPass;

  public enum Target {
    GINGER, ZAATAR
  }

  /**
   * State determining whether we are compiling from SFDL without profiling information
   */
  public static boolean isFirstPass() {
    return isFirstPass;
  }
  public static void setFirstPass(boolean firstPass) {
    isFirstPass = firstPass;
  }
}
