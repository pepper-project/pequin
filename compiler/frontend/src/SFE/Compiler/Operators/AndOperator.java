// PlusOperator.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler.Operators;

import SFE.Compiler.BooleanConstant;
import SFE.Compiler.BooleanExpressions;
import SFE.Compiler.BooleanType;
import SFE.Compiler.Expression;
import SFE.Compiler.PolynomialExpression;
import SFE.Compiler.SLPTReduction;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;


/**
 * A class for representing binary & operator expressions that can be defined
 * in the program.
 */
public class AndOperator extends ScalarOperator implements SLPTReduction {
  //~ Methods ----------------------------------------------------------------

  /**
   * Returns a string representation of the object.
   */
  public String toString() {
    return "&";
  }

  public int arity() {
    return 2;
  }

  public Expression inlineOp(StatementBuffer sb, Expression ... args) {
    return resolve(args);
  }
  
  public Expression resolve(Expression... args) {
    Expression left = args[0];
    Expression right = args[1];

    BooleanConstant lc = BooleanConstant.toBooleanConstant(left);
    //Short circuit from left to right.
    if (lc != null && lc.value() == 0){
      return BooleanConstant.FALSE;
    }
    BooleanConstant rc = BooleanConstant.toBooleanConstant(right);
    if (lc != null && rc != null){
      return new BooleanConstant(lc.value() * rc.value() == 1);
    }
    if (lc != null && lc.value() == 1){
      return right;
    }
    if (rc != null && rc.value() == 1){
      return left;
    }
    if (rc != null && rc.value() == 0){
      return BooleanConstant.FALSE;
    }
    
    PolynomialExpression pleft = PolynomialExpression.toPolynomialExpression(left);
    PolynomialExpression pright = PolynomialExpression.toPolynomialExpression(right);
    if (pleft != null && pright != null) {
      if (pleft.getDegree() + pright.getDegree() <= 2) {
        return BooleanExpressions.and(pleft, pright);
      }
    }

    return null;
  }

  public Type getType(Object obj) {
    return new BooleanType();
  }

  /**
   * Returns an int that represents the priority of the operator
   * @return an int that represents the priority of the operator
   */
  public int priority() {
    return 0;
  }

}
