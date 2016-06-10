// ConstExpression.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.Collection;
import java.util.Collections;
import java.util.Vector;

import SFE.Compiler.Operators.UnaryPlusOperator;

/**
 * The ConstExpression class represents consts expressions that
 * can appear in the program.
 */
public abstract class ConstExpression extends Expression implements Inlineable {
  public ConstExpression() {
  }
  
  //~ Methods ----------------------------------------------------------------

  public Expression changeReference(VariableLUT unique) {
    //Const expressions dont reference other variables
    return this;
  }


  public Collection<LvalExpression> getLvalExpressionInputs() {
    return Collections.<LvalExpression>emptyList();
  }

  public Expression inline(Object obj, StatementBuffer constraints) {
    //Default implementation returns this
    return this;
  }

  public ConstExpression fieldEltAt(int i) {
    throw new RuntimeException("Not yet implemented");
  }

  /*
  public Vector<ConstExpression> getDerivedCvalues() {
    return getType().getDerivedCvalues(this);
  }
  */

  public static ConstExpression toConstExpression(Expression c) {
    if (c instanceof ConstExpression) {
      return (ConstExpression)c;
    }
    if (c instanceof UnaryOpExpression) {
      UnaryOpExpression uo = ((UnaryOpExpression)c);
      return toConstExpression(uo.getOperator().resolve(uo.getMiddle()));
    }
    if (c instanceof BinaryOpExpression) {
      BinaryOpExpression bo = ((BinaryOpExpression)c);
      return toConstExpression(bo.getOperator().resolve(bo.getLeft(), bo.getRight()));
    }
    AssignmentStatement as = AssignmentStatement.getAssignment(c);
    if (as != null){
      for(Expression q : as.getAllRHS()){
        ConstExpression got = toConstExpression(q);
        if (got != null){
          return got;
        }
      }
    }
    return null;
  }
}
