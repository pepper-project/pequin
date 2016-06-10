package SFE.Compiler;

import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import SFE.Compiler.Operators.UnaryPlusOperator;

/**
 * An assignment statement with a left hand side which has yet to be connected to an LValue.
 *
 * Currently, the array statement must be resolved into a true ArrayStatement during uniqueVars.
 * If it is not possible to resolve this statement to an ArrayStatement during compilation,
 * an expensive technique is used to turn this statement into a dynamic assignment, where all possible
 * assignment targets are assigned a conditional assignment statement.
 * 
 * @deprecated - Currently ProtoAssignmentStatements with an ArrayAccessOperator LHS are the preferred way to do this
 */
public class ArrayStatement extends Statement implements SLPTReduction, Optimizable {
  private LvalExpression base;
  private String varName;
  private Vector<Expression> expressions;
  private Vector<Integer> lengths;
  private OperationExpression rhs;

  public ArrayStatement(LvalExpression base, Vector<Expression> expressions, Vector lengths,
                        OperationExpression rhs) {
    this.base = base;
    this.varName = base.getName();
    this.expressions = new Vector();
    for(int i = 0; i < expressions.size(); i++) {
      Expression expr = expressions.get(i);
      if (expr instanceof OperationExpression) {
        this.expressions.add((OperationExpression)expr);
      } else {
        this.expressions.add(new UnaryOpExpression(new UnaryPlusOperator(), expr));
      }
    }
    this.lengths = lengths;
    this.rhs = rhs;
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Transforms this multibit ArrayStatement into singlebit Statements
   * and returns a BlockStatement containing the result.
   * @param obj not used (null).
   * @return BlockStatement containing singlebit Statements
   *                 of this ArrayStatement.
   */
  public BlockStatement toSLPTCircuit(Object obj) {
    //Hmm...
    //return ((SLPTReduction) rhs).toSLPTCircuit(this);
    BlockStatement toRet = new BlockStatement();

    //rhs = new UnaryOpExpression(new UnaryPlusOperator(), rhs.evaluateExpression(varName, toRet));

    for(int i = 0; i < expressions.size(); i++) {
      Expression before = expressions.get(i);
      //Each expression must evaluate to an index, which has size 1.
      Expression after = before.evaluateExpression(varName, "ase"+i, toRet); //performs toSLPT conversion
      //after = after.bitAt(0);
      if (after instanceof OperationExpression) {
        expressions.set(i, (OperationExpression)after);
      } else {
        expressions.set(i,new UnaryOpExpression(new UnaryPlusOperator(), after));
      }
    }
    toRet.addStatement(this);
    return toRet;
  }

  /**
   * Returns a string representation of this ArrayStatement.
   * @return a string representation of this ArrayStatement.
   */
  public String toString() {
    return varName + expressions + '=' + rhs.toString() + "\n";
  }

  /**
   * Unique vars transformations.
   */
  public Statement uniqueVars() {
    if (expressions.size() == 0) {
      throw new RuntimeException("Assertion error");
    }

    //Apply unique vars to the expressions
    for(int i = 0; i < expressions.size(); i++) {
      Expression after = expressions.get(i).changeReference(Function.getVars());
      expressions.set(i, after);
    }
    //At this point, any of the references in any expression in expressions should be resolvable.
    int[] indexes = new int[lengths.size()];
    boolean isCompileTimeIndirection = true;
    for(int i = 0; i < expressions.size(); i++) {
      Expression before = expressions.get(i);
      IntConstant asIntConst = IntConstant.toIntConstant(before);
      if (asIntConst != null) {
        indexes[i] = asIntConst.toInt();
      } else {
        throw new RuntimeException("Invalid array index, " + before+" could not be resolved to an int");
      }
    }

    //Then lookup the LHS and return an AssignmentStatement.
    //We run toSLPT on the assignment statement, and then unique vars on it.
    return new AssignmentStatement(accessLval(indexes),rhs).toSLPTCircuit(null);
  }

  /**
   * Advances the indexes of an array variable.
   * @param indexes indeces of the array variable.
   * @param lengths limit of each index in the array
   * @return false if all indeces get to the limit.
   */
  private static boolean advanceIndexes(int[] indexes, Vector lengths) {
    int i = indexes.length - 1;

    while (i >= 0) {
      indexes[i] =
        (indexes[i] + 1) % ((Integer) lengths.elementAt(i)).intValue();

      if (indexes[i] == 0) {
        i--;
      } else {
        return true;
      }
    }

    return false;
  }
  /**
   * Fills in the specified indexes of an indirect variable name
   *
   * For example, fillInIndices({a,,,.value}, {1,2,3}) becomes a[1][2][3].value
   *
   * This method does proper array bounds checking.
   * @param lengths2
   */
  private LvalExpression accessLval(int[] indexes) {
    String[] varNameSplited = (varName+"$").split("\\[\\$\\]");
    //System.out.println(Arrays.toString(varNameSplited));
    StringBuffer sb = new StringBuffer();
    for (int i = 0; i < indexes.length; i++) {
      if (indexes[i] < 0 || indexes[i] >= lengths.get(i)) {
        throw new ArrayIndexOutOfBoundsException(Arrays.toString(indexes)+" is not a valid indexing into array "+varName+", index #"+(i+1)+" is out of range.");
      }

      sb.append(varNameSplited[i]);
      sb.append("[");
      sb.append(indexes[i]);
      sb.append("]");
    }

    if (varNameSplited.length > indexes.length) {
      sb.append(varNameSplited[varNameSplited.length - 1]);
    }

    String adjustedName = sb.toString();
    adjustedName = adjustedName.substring(0,adjustedName.length() - 1);
    return Function.getVars().getVar(adjustedName);
  }

  /**
   * Returns a replica this statement.
   * @return a replica this statement.
   */
  public Statement duplicate() {
    Vector nExpressions = new Vector();
    for(Expression k : expressions) {
      nExpressions.add(k.duplicate());
    }

    return new ArrayStatement(
             base,
             nExpressions,
             lengths,
             (OperationExpression) rhs.duplicate());
  }

  public void optimize(Optimization job) {
    switch(job) {
    case DUPLICATED_IN_FUNCTION:
      rhs.duplicatedInFunction();
      for(Expression e: expressions) {
        e.duplicate();
      }
      //Don't worry about lhs
      break;
    default:
      throw new RuntimeException("Not yet implemented");
    }
  }

  public void blockOptimize(BlockOptimization job, List body) {
    throw new RuntimeException("Not yet implemented");
  }

  public void buildUsedStatementsHash() {
    throw new RuntimeException("Not yet implemented");
  }

  public void toAssignmentStatements(StatementBuffer assignments) {
    uniqueVars().toAssignmentStatements(assignments);
  }
}
