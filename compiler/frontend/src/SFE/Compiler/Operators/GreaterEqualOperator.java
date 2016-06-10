// GreaterEqualOperator.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler.Operators;

import SFE.Compiler.AssignmentStatement;
import SFE.Compiler.BinaryOpExpression;
import SFE.Compiler.BlockStatement;
import SFE.Compiler.BooleanType;
import SFE.Compiler.Expression;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.SLPTReduction;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;


/**
 * A class for representing '&gt;=' operator expressions that can be defined
 * in the program.
 */
public class GreaterEqualOperator extends ScalarOperator implements SLPTReduction {
  //~ Methods ----------------------------------------------------------------

  /**
   * Returns a string representation of the object.
   */
  public String toString() {
    return ">=";
  }

  /**
   * Returns 2 as the arity of this PlusOperator.
   * Arity is 1 for unary ops; 2 for binary ops; 3 for ternary ops; 0 for constants
   * @return 2 as the arity of this PlusOperator.
   */
  public int arity() {
    return 2;
  }

  /**
   * Transforms this multibit expression into singlebit statements
   * and returns the result. Note:  x&gt;=y &lt;==&gt; !(x&lt;y).
   * @param obj the AssignmentStatement that holds this GreaterOperator.
   * @return a BlockStatement containing the result transformation.
   */
  public BlockStatement toSLPTCircuit(Object obj) {
    AssignmentStatement as     = ((AssignmentStatement) obj);
    LvalExpression      lhs    = as.getLHS(); //LHS of the param statement
    BinaryOpExpression  rhs    = (BinaryOpExpression) (as.getRHS());
    BlockStatement      result = new BlockStatement();

    Expression          right = rhs.getRight();
    Expression          left  = rhs.getLeft();

    result.addStatement(new AssignmentStatement(
                          lhs,
                          new BinaryOpExpression(new LessEqualOperator(), right, left) //Switch order of operators and use less operator
                        ).toSLPTCircuit(null));
    return result;
  }

  /**
   * Returns an int (1) that represents the priority of the operator
   * @return an int (1) that represents the priority of the operator
   */
  public int priority() {
    return 1;
  }

  public Type getType(Object obj) {
    return new BooleanType();
  }

  public Expression inlineOp(StatementBuffer assignments, Expression... args) {
    throw new RuntimeException("Not implemented");
  }

  public Expression resolve(Expression... args) {
    return new LessEqualOperator().resolve(args[1], args[0]);
  }
}
