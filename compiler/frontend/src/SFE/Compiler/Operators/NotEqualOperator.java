// NotEqualOperator.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler.Operators;

import java.io.PrintWriter;

import SFE.Compiler.AssignmentStatement;
import SFE.Compiler.BinaryOpExpression;
import SFE.Compiler.BlockStatement;
import SFE.Compiler.BooleanConstant;
import SFE.Compiler.BooleanType;
import SFE.Compiler.Expression;
import SFE.Compiler.FloatConstant;
import SFE.Compiler.IntConstant;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.OutputWriter;
import SFE.Compiler.SLPTReduction;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;
import SFE.Compiler.TypeHeirarchy;



/**
 * A class for representing not equal operator expressions that can be defined
 * in the program.
 */
public class NotEqualOperator extends ScalarOperator implements OutputWriter, SLPTReduction {
  //~ Methods ----------------------------------------------------------------

  /**
   * Returns a string representation of the object.
   */
  public String toString() {
    return "!=";
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

  private static boolean dualVariableInput;
  public static void setDualVariableInput(boolean b) {
    dualVariableInput = b;
  }

  /**
   * Transforms this multibit expression into singlebit statements
   * and returns the result.
   * Note:  !x==y &lt;==&gt; x!=y.
   * @param obj the AssignmentStatement that holds this GreaterOperator.
   * @return a BlockStatement containing the result statements.
   */
  public BlockStatement toSLPTCircuit(Object obj) {
    AssignmentStatement as     = ((AssignmentStatement) obj);
    LvalExpression      lhs    = as.getLHS(); //LHS of the param statement
    BinaryOpExpression  rhs    = (BinaryOpExpression) (as.getRHS());
    BlockStatement      result = new BlockStatement();

    Expression          right = rhs.getRight();
    Expression          left  = rhs.getLeft();

    if (dualVariableInput) {
      result.addStatement(new AssignmentStatement(
                            lhs.lvalFieldEltAt(0),
                            new BinaryOpExpression(this, left, right)
                          ));
    } else {
      //Take the difference of the left and right as the left operand, to avoid having dual variable input.
      result.addStatement(new AssignmentStatement(
                            lhs.lvalFieldEltAt(0),
                            new BinaryOpExpression(this,
                                new BinaryOpExpression(new MinusOperator(), left, right),
                                IntConstant.ZERO)
                          ));
    }


    return result;
  }

  public Expression inlineOp(StatementBuffer assignments, Expression ... args) {
    BooleanConstant bc = resolve(args);
    if (bc != null){
      return bc;
    }
    
    Expression left = args[0];
    Expression right = args[1];

    if (TypeHeirarchy.isSubType(left.getType(),new BooleanType()) &&
        TypeHeirarchy.isSubType(right.getType(),new BooleanType())) {
      //Return a xor b
      Expression tryXor = new XOROperator().inlineOp(assignments, left, right);
      if (tryXor != null) {
        return tryXor;
      }
    }
    
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
      return new BooleanConstant(valA.compareTo(valB) != 0);
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
    return 1;
  }

  public void toCircuit(Object obj, PrintWriter circuit) {
    BinaryOpExpression expr = ((BinaryOpExpression)obj);
    ((OutputWriter)expr.getLeft()).toCircuit(null, circuit);
    circuit.print(" != ");
    ((OutputWriter)expr.getRight()).toCircuit(null, circuit);
  }
}
