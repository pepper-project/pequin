// BinaryOpExpression.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Vector;

import SFE.Compiler.Operators.Operator;


/**
 * A class for representing binary operation expressions that can be defined
 * in the program.
 */
public class BinaryOpExpression extends OperationExpression implements Inlineable {
  //~ Instance fields --------------------------------------------------------

  // data members

  /*
   * The left input pin of this BinaryOpExpression
   */
  private Expression left;

  /*
   * The right input pin of this BinaryOpExpression
   */
  private Expression right;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a new BinaryOpExpression from a given oparator and inputs.
   * @param op the binary operator.
   * @param left the left input pin.
   * @param right the right input pin.
   */
  public BinaryOpExpression(Operator op, Expression left, Expression right) {
    super(op);
    this.left      = left;
    this.right     = right;

  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return (op).toString() + " inputs [ " +
           (left).toString() + " " +
           (right).toString() + " ]";
  }

  /**
   * Transforms this BinaryOpExpression into SLPT statements
   * and returns a BlockStatement containing the result.
   * @param obj the AssignmentStatement that holds this BinaryOpExpression.
   * @return BlockStatement containing singlebit Statements of this
   *                 BinaryOpExpression.
   */
  public BlockStatement toSLPTCircuit(Object obj) {
    AssignmentStatement as     = ((AssignmentStatement) obj);
    LvalExpression      lhs    = as.getLHS(); //LHS of the param statement
    BlockStatement      result = new BlockStatement();
    
    if (op instanceof CompileTimeOperator){
      result.addStatement(as);
      return result;
    }
    
    //FIAT: all binary operations currently work on size-1 operands. This is not the case for unary operations.
    Expression nLeft = left.evaluateExpression(lhs.getName(),"L", result); //produces SLPT circuit
    Expression nRight = right.evaluateExpression(lhs.getName(),"R", result); //produces SLPT circuit

    if (nLeft == null || nRight == null){
      throw new RuntimeException("Assertion error");
    }
    this.left = nLeft;
    this.right = nRight;
    result.addStatement(((SLPTReduction) op).toSLPTCircuit(obj));

    return result;
  }

  /**
   * Used during optimization (phase I, specifically).
   * Subexpressions / references to LValues with expressions that can be inlined should be inlined,
   * and the resulting expression returned.
   *
   * This implementation defers to op.toInlinedForm
   *
   * Parameter obj is ignored.
   */
  public Expression inline(Object obj, StatementBuffer assignments) {
    AssignmentStatement rAs = AssignmentStatement.getAssignment(right);
    AssignmentStatement lAs = AssignmentStatement.getAssignment(left);
    //Pattern 1:
    //left = <something>
    //right = <something>
    //current = left OP right
    //and both left, right are referenced exactly once (here).
    if (rAs != null && lAs != null) {
      if (rAs.getLHS().getReferenceCountUB() == 1 && lAs.getLHS().getReferenceCountUB() == 1) {
        for(Expression lVal : lAs.getAllRHS()) {
          for(Expression rVal : rAs.getAllRHS()) {
            Expression result = op.inlineOp(assignments, lVal, rVal);
            if (result != null) {
              assignments.callbackAssignment(lAs);
              assignments.callbackAssignment(rAs);
              return result;
            }
          }
        }
      }
    }

    //Pattern 1.5:
    //left = <something>
    //current = left OP left
    //and both left, right are referenced exactly twice (here).
    if (rAs != null && lAs != null) {
      if (rAs.getOutputLine() == lAs.getOutputLine()) {
        if (rAs.getLHS().getReferenceCountUB() == 2) {
          for(Expression rVal : rAs.getAllRHS()) {
            Expression result = op.inlineOp(assignments, rVal, rVal.duplicate());
            if (result != null) {
              assignments.callbackAssignment(rAs);
              return result;
            }
          }
        }
      }
    }

    //Pattern 2:
    //left = <something>
    //right is an expression
    //current = left OP right
    //and left is referenced exactly once (here).
    if (lAs != null) {
      if (lAs.getLHS().getReferenceCountUB() == 1) {
        for(Expression lVal : lAs.getAllRHS()) {
          Expression result = op.inlineOp(assignments, lVal, right);
          if (result != null) {
            assignments.callbackAssignment(lAs);
            return result;
          }
        }
      }
    }

    //Pattern 3:
    //left is an expression
    //right = <something>
    //current = left OP right
    //and right is referenced exactly once (here).
    if (rAs != null) {
      if (rAs.getLHS().getReferenceCountUB() == 1) {
        for(Expression rVal : rAs.getAllRHS()) {
          Expression result = op.inlineOp(assignments, left, rVal);
          if (result != null) {
            assignments.callbackAssignment(rAs);
            return result;
          }
        }
      }
    }

    //Finally, we are left with the case where left and right are expressions.
    //They must be inlined.
    return op.inlineOp(assignments, left, right);
  }

  /**
   * Returns the right input pin.
   * @return the right input pin.
   */
  public Expression getRight() {
    return right;
  }

  /**
   * Returns the left input pin.
   * @return the left input pin.
   */
  public Expression getLeft() {
    return left;
  }

  /**
   * Sets the left input pin
   */
  public void setLeft(Expression left) {
    this.left = left;
    if (right == null){
      throw new RuntimeException();
    }
  }

  /**
   * Sets the right input pin
   */
  public void setRight(Expression right) {
    this.right = right;
    if (right == null){
      throw new RuntimeException();
    }
  }

  public Type getType() {
    return op.getType(this);
  }

  /**
   * Writes this Expression to the output circuit.
   * @param circuit the output circuit file.
   */
  public void toCircuit(Object obj, PrintWriter circuit) {
    ((OutputWriter)op).toCircuit(this, circuit);
  }

  /**
   * Sorts the input gates according to their names and returns
   * the result OperationExpression. This method is used in the optimization
   * process.
   * @return the OperationExpression with the sorted inputs.
   */
  public OperationExpression sortInputs() {
    return this;
    /* TODO: sorted inputs would be nice.

    String leftStr  = left.toString();
    String rightStr = right.toString();


    if (leftStr.compareTo(rightStr) < 0) { // if left < right

    	Operator newOp = op.switchRightLeft();

    	// switch expressions and return result
    	return new BinaryOpExpression(newOp, right, left);
    }

    return this;

    */
  }

  /**
   * Returns an array of the input LvalExpressions of this gate.
   * This method is used in the second phase of the optimization.
   * @return an array of the input LvalExpressions of this gate.
   */
  public Collection<LvalExpression> getLvalExpressionInputs() {
    Vector result = new Vector();
    if (left instanceof LvalExpression) {
      result.add(left);
    }

    if (right instanceof LvalExpression) {
      result.add(right);
    }

    result.addAll(left.getLvalExpressionInputs());
    result.addAll(right.getLvalExpressionInputs());
    return result;
  }

  /**
   * Changes references of variables to the last place they were changed
   * @param unique holds all the variables and their references
   */
  public Expression changeReference(VariableLUT unique) {
    left = left.changeReference(unique);
    right = right.changeReference(unique);

    return this;
  }

  public void duplicatedInFunction() {
    left.duplicatedInFunction();
    right.duplicatedInFunction();
  }

  /**
   * returns a replica of this BinaryOpExpression.
   * @return a replica of this BinaryOpExpression.
   */
  public Expression duplicate() {
    return new BinaryOpExpression(op, left.duplicate(), right.duplicate());
  }

  public int size() {
    return getType().size();
  }
  public Expression fieldEltAt(int i) {
    return op.fieldEltAt(this, i);
  }

}
