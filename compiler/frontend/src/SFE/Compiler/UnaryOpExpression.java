// UnaryOpExpression.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.io.PrintWriter;
import java.util.Vector;

import SFE.Compiler.Operators.Operator;
import SFE.Compiler.Operators.UnaryPlusOperator;

/**
 * A class for representing unary operator expressions that can be defined
 * in the program.
 */
public class UnaryOpExpression extends OperationExpression implements Inlineable {
  //~ Instance fields --------------------------------------------------------

  // data members

  /*
   * Hold the input of this expression.
   */
  private Expression middle;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a new UnaryOpExpression from a given oparator and input.
   * @param op the unary operator.
   * @param middle the input.
   */
  public UnaryOpExpression(Operator op, Expression middle) {
    super(op);
    this.middle     = middle;
    if (middle == null){
      throw new RuntimeException();
    }
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Returns a string representation of the object.
   *
   *
   * Returns a string representing this object as it appear at the
   * output circuit.
   * @return a string representing this object as it appear at the
   * output circuit.
   */
  public String toString() {
    if ((op instanceof UnaryPlusOperator) &&
        middle instanceof ConstExpression) {
      return "gate arity 0 table [" + (middle).toString() +
             "] inputs []";
    }

    return (op).toString() + " inputs [ " +
           (middle).toString() + " ]";
  }

  /**
   * Transforms this multibit expression into singlebit statements
   * and adds them to the appropriate function.
   * @param obj the AssignmentStatement that holds this UnaryOpExpression.
   */
  public BlockStatement toSLPTCircuit(Object obj) {
    BlockStatement      result = new BlockStatement();
    
    if (op instanceof CompileTimeOperator){
      result.addStatement((AssignmentStatement)obj);
      return result;
    }

    result.addStatement(((SLPTReduction) op).toSLPTCircuit(obj));
    return result;
  }

  /**
   * return the input of the expression
   * @return Expression the input of the unary expression
   */
  public Expression getMiddle() {
    return middle;
  }

  public Expression inline(Object obj, StatementBuffer assignments) {
    AssignmentStatement mAs = AssignmentStatement.getAssignment(middle);
    //Pattern 1:
    //middle = <something>
    //current = OP middle
    //and middle is referenced exactly once (here).
    if (mAs != null) {
      if (mAs.getLHS().getReferenceCountUB() == 1) {
        for(Expression mVal : mAs.getAllRHS()) {
          Expression result = op.inlineOp(assignments, mVal);
          if (result != null) {
            assignments.callbackAssignment(mAs);
            return result;
          }
        }
      }
    }

    //Finally, we are left with the case where middle is an expression
    //It must be inlined
    return op.inlineOp(assignments, middle);
  }

  public Type getType() {
    return op.getType(this);
  }

  /**
   * Returns a string representing this object as it appear at the
   * output circuit.
   * @return a string representing this object as it appear at the
   * output circuit.
   */
  public void toCircuit(Object obj, PrintWriter circuit) {
    ((OutputWriter)op).toCircuit(this, circuit);
  }

  /**
   * Sorts the input gates according to their names and returns
   * the result OperationExpression. This method is used in the optimization
   * process. In UnaryOpExpression there is nothing to sort since it
   * has only one input.
   * @return the OperationExpression with the sorted inputs (this).
   */
  public OperationExpression sortInputs() {
    return this;
  }

  /**
   * Returns an array of the input LvalExpressions of this gate.
   * This method is used in the second phase of the optimization.
   * @return an array of the input LvalExpressions of this gate.
   */
  public Vector getLvalExpressionInputs() {
    Vector result = new Vector();

    if (middle instanceof LvalExpression) {
      result.add(middle);
    }

    result.addAll(middle.getLvalExpressionInputs());

    return result;
  }

  public Expression changeReference(VariableLUT unique) {
    middle = middle.changeReference(unique);
    return this;
  }

  public void duplicatedInFunction() {
    middle.duplicatedInFunction();
  }

  /**
   * returns a replica of this expression
   * @return a replica of this expression
   */
  public Expression duplicate() {
    return new UnaryOpExpression(op, middle.duplicate());
  }

  public Expression fieldEltAt(int i) {
    return op.fieldEltAt(this, i);
  }

  public int size() {
    return getType().size();
  }
}
