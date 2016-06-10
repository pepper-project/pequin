// Operator.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler.Operators;

import SFE.Compiler.Expression;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;


/**
 * Abstract class for representing an operator in the program.
 * All operators have a name (whose interpretation depends on
 * the subclass), as well as abstract functions for defining
 * the semantics of the particular operator subclass.
 */
public abstract class Operator {
  //~ Methods ----------------------------------------------------------------

  /**
   * Returns the arity of the operator
   * 1 for unary ops; 2 for binary ops; 3 for ternary ops; 0 for constants
   * @return the arity of the operator
   */
  public abstract int arity();

  /**
   * Returns an int theat represents the priority of the operator
   * @return an int theat represents the priority of the operator
   */
  public abstract int priority();

  /**
   * Get the type of this operator
   *
   * obj is a context variable
   */
  public abstract Type getType(Object obj);

  /**
   * If the operation applied to the arguments can be expressed as a single RHS,
   * return the expression in that form (i.e. perform a coercion to that form).
   *
   * Otherwise, return null.
   * 
   * In performing this coercion, assignments and variables may be generated. These variables will be
   * temporary variables (with uniquified names) for the variable with name "prefix"
   * 
   * If a non-null value is returned, the returned expressions MUST BE USED in place of the old expression.
   */
  public abstract Expression inlineOp(StatementBuffer assignments, Expression ... args);
  
  /**
   * If the operation applied to the arguments can be performed now (i.e. at compile time), 
   * perform the operation and return the resulting expression.
   * 
   * Otherwise, return null.
   * 
   * The difference between this call and inlineOp is:
   *   1) The result need not be a valid RHS for conversion to constraints
   *   2) inlineOp may return trivially - i.e. return an expression that simply defers the
   *     operation until after compile time. This method will make progress, or return null.
   *   3) inlineOp has more power, because it can generate assignments and variables. 
   */
  public abstract Expression resolve(Expression ... args);

  /**
   * Return an expression representing the ith field elt of the result of this expression
   */
  public abstract Expression fieldEltAt(Expression expression, int i);
}