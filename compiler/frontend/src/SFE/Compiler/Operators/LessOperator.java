// LessOperator.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler.Operators;

import java.io.PrintWriter;
import SFE.Compiler.BinaryOpExpression;
import SFE.Compiler.BooleanConstant;
import SFE.Compiler.BooleanType;
import SFE.Compiler.Expression;
import SFE.Compiler.FloatConstant;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.OutputWriter;
import SFE.Compiler.RestrictedUnsignedIntType;
import SFE.Compiler.SLPTReduction;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;


/**
 * A class for representing &lt; operator expressions that can be defined
 * in the program.
 */
public class LessOperator extends ScalarOperator implements OutputWriter, SLPTReduction {
  //~ Methods ----------------------------------------------------------------

  /**
   * Returns a string representation of the object.
   */
  public String toString() {
    return "<";
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

  public Expression inlineOp(StatementBuffer assignments, Expression ... args) {
    BooleanConstant bc = resolve(args);
    if (bc != null){
      return bc;
    }
    
    Expression left = args[0];
    Expression right = args[1];

    Expression leftLvalConst = FloatConstant.toFloatConstant(left);
    Expression rightLvalConst = FloatConstant.toFloatConstant(right);

    if (leftLvalConst == null) {
      leftLvalConst = LvalExpression.toLvalExpression(left);
    }

    if (rightLvalConst == null) {
      rightLvalConst = LvalExpression.toLvalExpression(right);
    }

    if (leftLvalConst == null || rightLvalConst == null) {
      return null;
    }

    return new BinaryOpExpression(this, leftLvalConst, rightLvalConst);
  }
  public BooleanConstant resolve(Expression... args) {
    Expression left = args[0];
    Expression right = args[1];
    FloatConstant valA = FloatConstant.toFloatConstant(left);
    FloatConstant valB = FloatConstant.toFloatConstant(right);

    if (valA != null && valB != null) {
      return new BooleanConstant(valA.compareTo(valB) < 0);
    }
    if (valB != null && valB.isZero() && (left.getType() instanceof RestrictedUnsignedIntType)){
      return BooleanConstant.FALSE;
    }
    return null;
  }

  public Type getType(Object obj) {
    return new BooleanType();
  }

  /**
   * Returns an int theat represents the priority of the operator
   * @return an int theat represents the priority of the operator
   */
  public int priority() {
    return 1;
  }

  public void toCircuit(Object obj, PrintWriter circuit) {
    BinaryOpExpression expr = ((BinaryOpExpression)obj);
    ((OutputWriter)expr.getLeft()).toCircuit(null, circuit);
    circuit.print(" < ");
    ((OutputWriter)expr.getRight()).toCircuit(null, circuit);
  }
}
