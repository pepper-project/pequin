// MinusOperator.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler.Operators;

import SFE.Compiler.AssignmentStatement;
import SFE.Compiler.BinaryOpExpression;
import SFE.Compiler.BlockStatement;
import SFE.Compiler.Expression;
import SFE.Compiler.SLPTReduction;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;
import SFE.Compiler.UnaryOpExpression;

/**
 * A class that represents minus (-) operator that can be defined in the
 * program.
 */
public class MinusOperator extends ScalarOperator implements SLPTReduction {
  //~ Methods ----------------------------------------------------------------

  /**
   * Transforms this multibit expression into singlebit statements
   * and returns the result.
   * Note: x-y &lt;==&gt; x+(-y).
   * @param obj the AssignmentStatement that holds this MinusOperator.
   * @return a BlockStatement containing the result statements.
   */
  public BlockStatement toSLPTCircuit(Object obj) {
    //No conversion needed, plus gate is an arithmetic gate

    AssignmentStatement as     = ((AssignmentStatement) obj);
    BlockStatement      result = new BlockStatement();
    BinaryOpExpression rhs = (BinaryOpExpression)as.getRHS();

    Expression left = rhs.getLeft();
    Expression right = rhs.getRight();

    //Exchange the minus operator for a plus operator, negating the right input
    //Because this is an arithmetic op, the temporary variable holding the RIGHT must be of size 1.
    right = new UnaryOpExpression(new UnaryMinusOperator(), right);//.evaluateExpression(as.getLHS().getName(), 1, result);

    result.addStatement(new AssignmentStatement(as.getLHS(),
                        new BinaryOpExpression(
                          new PlusOperator(), left, right)).toSLPTCircuit(null));
    return result;
  }

  public Expression inlineOp(StatementBuffer assignments, Expression ... args) {
    Expression left = args[0];
    Expression right = args[1];

    return new PlusOperator().inlineOp(assignments, left, new UnaryOpExpression(new UnaryMinusOperator(), right));
  }

  public Expression resolve(Expression ... args) {
    Expression left = args[0];
    Expression right = args[1];

    return new PlusOperator().resolve(left, new UnaryOpExpression(new UnaryMinusOperator(), right));
  }
  

  /**
   * Returns 2 as the arity of this UnaryMinusOperator.
   * Arity is 1 for unary ops; 2 for binary ops; 3 for ternary ops; 0 for constants
   * @return 2 as the arity of this UnaryMinusOperator.
   */
  public int arity() {
    return 2;
  }

  /**
   * Returns a string representation of the object.
   */
  public String toString() {
    return "-";
  }

  /**
   * Returns an int theat represents the priority of the operator
   * @return an int theat represents the priority of the operator
   */
  public int priority() {
    return 2;
  }

  public Type getType(Object obj) {
    throw new RuntimeException("Not implemented");
  }
}
