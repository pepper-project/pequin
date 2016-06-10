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
import SFE.Compiler.UnaryOperator;

/**
 * A class for representing binary ! operator expressions that can be defined
 * in the program.
 */
public class NotOperator extends ScalarOperator implements UnaryOperator, SLPTReduction {
  //~ Methods ----------------------------------------------------------------

  /**
   * Returns a string representation of the object.
   */
  public String toString() {
    return "!";
  }

  public int arity() {
    return 1;
  }

  public Expression inlineOp(StatementBuffer assignments, Expression ... args) {
    return resolve(args);
  }
  
  public Expression resolve(Expression ... args){
    Expression mid = args[0];

    BooleanConstant midc = BooleanConstant.toBooleanConstant(mid);
    if (midc != null){
      return new BooleanConstant((1 - midc.value()) == 1);
    }
    
    PolynomialExpression pmid = PolynomialExpression.toPolynomialExpression(mid);
    if (pmid != null) {
      if (pmid.getDegree() <= 2) {
        return BooleanExpressions.not(pmid);
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
    return 4;
  }
}
