// Expression.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.Collection;

/**
 * Abstract class for representing expressions that can be defined
 * in the program.
 */
public abstract class Expression {
  //~ Methods ----------------------------------------------------------------
  
  //A variable with name A:B is a temporary variable serving to compute variable A.
  public static final String TEMPORARY_SEPARATOR = ":";

  /**
   * Returns the number of field elements used to represent the result of this expression.
   */
  public abstract int size();
  
  /**
   * Returns an expression representing the ith field element used to represent the result
   * of this expression
   */
  public abstract Expression fieldEltAt(int i);
  
  /**
   * Recursively calculates expression and stores the result as a single field element. 
   * This implementation returns this. Expressions that return something more complicated
   * should override this method.
   *
   * The name of the intermediate variable created is (goal):(tmpName)
   *
   * @param as the AssignmentStatement that holds this expression (as rhs).
   * @param size - the size of the temporary variable (must be known before hand)
   * @param result the BlockStatement to hold the result in.
   * @return the result expression.
   */
  public Expression evaluateExpression(String goal, String tempName, BlockStatement result) {
    return this;
  }

  /**
   * Tells the expression that it has been duplicated in a function.
   *
   * Used for static_function operations
   */
  public void duplicatedInFunction() {
    //Nothing.
  }

  /**
     * Change the references this expression makes, using unique to resolve variables,
     * and return the resulting expression.
     */
  public abstract Expression changeReference(VariableLUT unique);

  /**
     * Returns an array of the input LvalExpressions of this expression
     * which are not already marked as being referenced (have a zero
     * reference count.).
     *
     * It is O.K. for implementations of this method to return lvalexpressions
     * which have a nonzero reference count as well. But this leads to unecessary
     * work, as marking something referenced twice is equivalent to once.
  public abstract Collection<LvalExpression> getUnrefLvalInputs();
     */
  
  /**
   * It is O.K. to return an lval twice in this list.
   * However, this method must be deterministic (i.e. as long as the expression is the same, this method returns the same thing)
   */
  public abstract Collection<LvalExpression> getLvalExpressionInputs();

  /**
   * Returns this expression. Expression are not duplicated.
   * @return this expression. Expression are not duplicated.
   */
  public Expression duplicate() {
    return this;
  }

  /**
   * Returns the type of this expression.
   */
  public abstract Type getType();

  /**
   * Context variable, used by various compilers to add metadata to expressions.
   */
  public Type metaType;

  public static Expression fullyResolve(Expression c) {
    if (c instanceof UnaryOpExpression) {
      UnaryOpExpression uo = ((UnaryOpExpression)c);
      Expression further = fullyResolve(uo.getOperator().resolve(uo.getMiddle()));
      if (further != null){
        return further;
      }
    } else if (c instanceof BinaryOpExpression) {
      BinaryOpExpression bo = ((BinaryOpExpression)c);
      Expression further = fullyResolve(bo.getOperator().resolve(bo.getLeft(), bo.getRight()));
      if (further != null){
        return further;
      }
    }
    return c;
  }
}
