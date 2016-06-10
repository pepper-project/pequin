// EqualOperator.java.
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
import SFE.Compiler.UnaryOpExpression;


/**
 * A class for representing == operator expressions that can be defined
 * in the program.
 */
public class EqualOperator extends ScalarOperator implements SLPTReduction {
  //~ Methods ----------------------------------------------------------------

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return "==";
  }

  /**
   * Returns 2 as the arity of this PlusOperator.
   * Arity is 1 for unary ops; 2 for binary ops; 3 for ternary ops;
   * 0 for constants
   * @return 2 as the arity of this PlusOperator.
   */
  public int arity() {
    return 2;
  }

  /**
   * Transforms this multibit expression into singlebit statements
   * and adds them to the appropriate function.
   * Note: x&lt;=y and x &gt;=y &lt;==&gt; x==y.
   * @param obj the AssignmentStatement that holds this GreaterOperator.
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
                          new UnaryOpExpression(new NotOperator(),new BinaryOpExpression(new NotEqualOperator(), left, right))
                        ).toSLPTCircuit(null));
    return result;
  }

  public Type getType(Object obj) {
    return new BooleanType();
  }

  /**
   * Returns 1 - The priority of this operator.
   * @return 1 - The priority of this operator.
   */
  public int priority() {
    return 1;
  }

  public Expression inlineOp(StatementBuffer assignments, Expression... args) {
    throw new RuntimeException("Not implemented");
  }

  public Expression resolve(Expression... args) {
    Expression NotEqual = new NotEqualOperator().resolve(args[0],args[1]);
    if (NotEqual != null){
      return new NotOperator().resolve(NotEqual);
    }
    return null;
  }
}
