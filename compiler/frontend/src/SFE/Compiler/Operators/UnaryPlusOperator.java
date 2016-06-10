// UnaryMinusOperator.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler.Operators;

import SFE.Compiler.AssignmentStatement;
import SFE.Compiler.BlockStatement;
import SFE.Compiler.Expression;
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
public class UnaryPlusOperator extends Operator implements SLPTReduction, UnaryOperator, OutputsToPolynomial {
  //~ Methods ----------------------------------------------------------------

  /**
   * Transforms this expression into an equivalent single level circuit of constraint-reducible gates
   */
  public BlockStatement toSLPTCircuit(Object obj) {
    AssignmentStatement as     = ((AssignmentStatement) obj);
    BlockStatement      result = new BlockStatement();
    LvalExpression lhs = as.getLHS();

    UnaryOpExpression rhs = (UnaryOpExpression)as.getRHS();
    Expression middle = rhs.getMiddle();
    
    if (middle.size() > lhs.size()){
      throw new RuntimeException("Cannot assign "+middle.getType()+" to "+lhs.getDeclaredType()+".");
    }
    
    if (lhs.size() > 1) {
      //Expand nonprimitive type (array, struct) happens here.
      for(int i = 0; i < lhs.size(); i++) {
        //result.addStatement(as);
        AssignmentStatement subAs = new AssignmentStatement(
          lhs.lvalFieldEltAt(i),
          new UnaryOpExpression(this, middle.fieldEltAt(i))
        );
        result.addStatement(subAs.toSLPTCircuit(null));
      }
    } else {
      middle = middle.evaluateExpression(lhs.getName(), "M", result);
      AssignmentStatement subAs = new AssignmentStatement(lhs.lvalFieldEltAt(0),new UnaryOpExpression(this, middle.fieldEltAt(0)));
      result.addStatement(subAs);
    }

    return result;
  }

  public Expression inlineOp(StatementBuffer assignments, Expression ... args) {
    return resolve(args);
  }
  public Expression resolve(Expression ... args) {
    return args[0];
  }

  public Type getType(Object obj) {
    UnaryOpExpression expr = (UnaryOpExpression) obj;

    return expr.getMiddle().getType();
  }

  /**
   * Returns 1 as the arity of this UnaryPlusOperator.
   * Arity is 1 for unary ops; 2 for binary ops; 3 for ternary ops; 0 for constants
   * @return 1 as the arity of this UnaryPlusOperator.
   */
  public int arity() {
    return 1;
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return "+";
  }

  /**
   * Returns an int theat represents the priority of the operator
   * @return an int theat represents the priority of the operator
   */
  public int priority() {
    return 2;
  }

  /**
   * Returns null if c is not a UnaryOpExpression
   * Otherwise, returns null if c is not an identity expression
   * Otherwise, returns the value of the identity expression
   */
  public static Expression getIdentityInnerValue(Expression c) {
    if (c instanceof UnaryOpExpression) {
      UnaryOpExpression oc = (UnaryOpExpression)c;
      if (oc.op instanceof UnaryPlusOperator) {
        return oc.getMiddle();
      }
    }
    return null;
  }

  public Expression fieldEltAt(Expression obj, int i) {
    //Just a wrapper.
    UnaryOpExpression expr = (UnaryOpExpression) obj;
    return expr.getMiddle().fieldEltAt(i);
  }
}
