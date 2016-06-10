package SFE.Compiler;

import java.util.List;
import java.util.Random;
import java.util.Vector;

public class FunctionBody extends Statement {
  private BlockStatement statements;
  private Function myFunc;

  /*
   * Constructors
   */
  public FunctionBody(Function function) {
    myFunc = function;
    statements = new BlockStatement(1);
  }

  /*
   * Methods
   */

  public void toAssignmentStatements(StatementBuffer assignments) {
    Random rgen = new Random(myFunc.randomSeed) {
      public String toString() {
        return myFunc.getName()+"_FUNCTION_STATIC_RANDOM_INT";
      }
    };
    Function.staticFunctionRandom.push(rgen);
    Consts.addConst(rgen.toString(), new ArrayConstant(new IntType()), true); //Make a new list to hold any function static random access in this function call
    statements.toAssignmentStatements(assignments);
    Function.staticFunctionRandom.pop();
  }

  /*
  public void toCircuit(PrintWriter circuit) {
  	// Reset the static field counter for this function:
  	Function.staticFunctionRandom.push(new Random(myFunc.randomSeed));
  	statements.toCircuit(circuit);
  	Function.staticFunctionRandom.pop();
  }
  */

  public Statement duplicate() {
    FunctionBody result = new FunctionBody(myFunc);
    result.statements = (BlockStatement)statements.duplicate();
    return result;
  }

  public Statement toSLPTCircuit(Object obj) {
    statements = statements.toSLPTCircuit(obj);

    return this.duplicate();
  }

  /*
  public Statement uniqueVars() {
  	statements = (BlockStatement)statements.uniqueVars();

  	return this;
  }
  */

  public List<Statement> getStatements() {
    return statements.getStatements();
  }

  public void addStatement(Statement statement) {
    statements.addStatement(statement);
  }

  public void addStatements(Vector statements) {
    this.statements.addStatements(statements);
  }
}
