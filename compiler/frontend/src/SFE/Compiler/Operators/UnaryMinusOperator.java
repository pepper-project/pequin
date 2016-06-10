// UnaryMinusOperator.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler.Operators;

import SFE.Compiler.AssignmentStatement;
import SFE.Compiler.BinaryOpExpression;
import SFE.Compiler.BlockStatement;
import SFE.Compiler.Expression;
import SFE.Compiler.IntConstant;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.OutputsToPolynomial;
import SFE.Compiler.SLPTReduction;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;
import SFE.Compiler.UnaryOpExpression;
import SFE.Compiler.UnaryOperator;



/**
 *
 */
public class UnaryMinusOperator extends ScalarOperator implements SLPTReduction, UnaryOperator, OutputsToPolynomial {
  //~ Methods ----------------------------------------------------------------

  /**
   * Transforms this expression into an equivalent single level circuit of constraint-reducible gates
   */
  public BlockStatement toSLPTCircuit(Object obj) {
    //No conversion needed, plus gate is an arithmetic gate

    AssignmentStatement as     = ((AssignmentStatement) obj);
    LvalExpression      lhs    = as.getLHS(); //LHS of the param statement
    BlockStatement      result = new BlockStatement();

    UnaryOpExpression rhs = (UnaryOpExpression)as.getRHS();

    Expression middle = rhs.getMiddle();
    //Arithmetic type - so the intermediate variable holding the middle must be of size 1
    middle = middle.evaluateExpression(lhs.getName(), "M", result);

    result.addStatement(new AssignmentStatement(as.getLHS(),
                        new BinaryOpExpression(
                          new TimesOperator(), IntConstant.NEG_ONE, middle)).toSLPTCircuit(null));

    return result;
  }

  public Expression inlineOp(StatementBuffer assignments, Expression ... args) {
    Expression middle = args[0];

    return new TimesOperator().inlineOp(assignments, IntConstant.NEG_ONE, middle);
  }  
  public Expression resolve(Expression ... args) {
    Expression middle = args[0];

    return new TimesOperator().resolve(IntConstant.NEG_ONE, middle);
  }

  public Type getType(Object obj) {
    throw new RuntimeException("Not implemented");
  }

  /**
   * Returns 1 as the arity of this UnaryMinusOperator.
   * Arity is 1 for unary ops; 2 for binary ops; 3 for ternary ops; 0 for constants
   * @return 1 as the arity of this UnaryMinusOperator.
   */
  public int arity() {
    return 1;
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
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
}
