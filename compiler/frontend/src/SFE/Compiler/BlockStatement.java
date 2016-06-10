// BlockStatement.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A class for representing a block of statements that can be defined
 * in the program.
 */
public class BlockStatement extends Statement implements Optimizable {
  //~ Instance fields --------------------------------------------------------

  // data members

  /*
   * Holds the statements defined in the block.
   */
  private List<Statement> statements;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a new BlockStatement.
   */
  public BlockStatement() {
    this(1);
  }
  public BlockStatement(int capacity) {
    statements = new ArrayList(capacity);
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Appends the specified statement to the end of this BlockStatement.
   * @param statement a statement to be appended to this BlockStatement.
   */
  public void addStatement(Statement statement) {
    if (statement instanceof BlockStatement) {
      addStatements(((BlockStatement)statement).getStatements());
    } else {
      statements.add(statement);
    }
  }

  /**
   * adds a given vector of statements into the block.
   * @param v the given vector of statements.
   */
  public void addStatements(Collection v) {
    for(Statement s : (Collection<Statement>)v) {
      addStatement(s);
    }
  }

  public List<Statement> getStatements() {
    return statements;
  }

  /**
   * Returns a string representation of the BlockStatement.
   * @return a string representation of the BlockStatement.
   */
  public String toString() {
    String str = new String();
    str += "{";

    for (int i = 0; i < statements.size(); i++)
      str += ((Statement) (statements.get(i))).toString();

    str += "}";

    return str;
  }

  /**
   * Transforms this multibit statements in this BlockStatement
   * into singlebit statements and returns the result.
   * @param obj not needed (null).
   * @return a BlockStatement containing the result of this transformation.
   */
  public BlockStatement toSLPTCircuit(Object obj) {
    BlockStatement result = new BlockStatement();

    for (int i = 0; i < statements.size(); i++) {
      result.addStatement(((SLPTReduction)(statements.get(i))).toSLPTCircuit(null));
    }

    return result;
  }

  /**
   * Writes this BlockStatement to the output circuit.
   * @param circuit the output circuit file.
   */
  /*
  public void toCircuit(PrintWriter circuit) {
  	for (int i = 0; i < statements.size(); i++)
  		((OutputWriter) (statements.get(i))).toCircuit(circuit);
  }
  */

  public void toAssignmentStatements(StatementBuffer assignments) {
    for (Statement s : statements) {
      s.toAssignmentStatements(assignments);
    }
  }

  /**
   * Unique vars transformations.
  public Statement uniqueVars() {
  	for (int i = 0; i < statements.size(); i++)
  		statements.set(i, ((Statement) statements.get(i)).uniqueVars());

  	return this;
  }
   */

  /**
   * Optimizes this BlockStatment.
   * runs the first optimization phaze on each of the statements in this
   * BlockStatement.
   */
  public void optimize(Optimization job) {
    //As long as all children implement the optimization, the block does.
    for (int i = 0; i < statements.size(); i++)
      ((Optimizable) (statements.get(i))).optimize(job);
  }

  /**
   * executes optimizePhaseII() on each of the statements in
   * this BlockStatement.
   */
  public void blockOptimize(BlockOptimization job, List newBody) {
    //As long as all children implement the optimization, the block does.
    for (int i = 0; i < statements.size(); i++) {
      ((Optimizable) (statements.get(i))).blockOptimize(job, newBody);
    }
  }


  /**
   * Executes buildUsedStatementsHash() for each statement in
   * the BlockStatement.
   */
  public void buildUsedStatementsHash() {
    for (int i = statements.size() - 1; i >= 0; i--)
      // Block/Assignment/Input Statement
      ((Optimizable) (statements.get(i))).buildUsedStatementsHash();
  }

  /**
   * returns a duplica of this BlockStatement.
   */
  public Statement duplicate() {
    BlockStatement result = new BlockStatement(statements.size());

    for (int i = 0; i < statements.size(); i++)
      result.addStatement(((Statement) statements.get(i)).duplicate());

    return result;
  }
}
