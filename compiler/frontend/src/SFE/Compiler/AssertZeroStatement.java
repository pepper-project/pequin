package SFE.Compiler;

import java.io.PrintWriter;

import SFE.Compiler.Operators.UnaryPlusOperator;

import ccomp.CBuiltinFunctions;

/**
 * A statement that asserts some input expression is 0.
 * This is a cheap way to manually introduce an arithmetic constraint from the C side.
 */
public class AssertZeroStatement extends StatementWithOutputLine implements OutputWriter{
  private Expression number;
  private int outputLine;
  
  public AssertZeroStatement(Expression number) {
    this.number = number;
  }

  public Statement toSLPTCircuit(Object obj) {
    BlockStatement result = new BlockStatement();

    // create a temp var that holds the number
    LvalExpression number_lval = Function.addTempLocalVar("assert_zero_tmp", new IntType());

    // create the assignment statement that assings the result
    AssignmentStatement conditionResultAs = new AssignmentStatement(number_lval,
        new UnaryOpExpression(new UnaryPlusOperator(), number));

    // evaluate the condition and stores it in the conditionResult
    result.addStatement(conditionResultAs.toSLPTCircuit(null));

    number = number_lval;

    result.addStatement(this);

    return result;
  }

  public Statement duplicate() {
    return new AssertZeroStatement(number.duplicate());
  }

  public void toAssignmentStatements(StatementBuffer assignments) {
    //Change refs
    number = number.changeReference(Function.getVars());
    
    toAssignmentStatements_NoChangeRef(assignments);
  }
  public void toAssignmentStatements_NoChangeRef(StatementBuffer assignments) {
    //Attempt to inline number as a constant
    FloatConstant ce = FloatConstant.toFloatConstant(number);
    if (ce != null){
      //Interesting scenario, we can at compile time determine whether this
      //assertion will flag.
      if (ce.isZero()){
        return; //This assertZero fizzles into nothingness.
      } else{
        throw new RuntimeException("AssertZero "+this+" always fails (value is "+ce+"), so stopping compilation.");
      }
    }
    
    if (!(number instanceof LvalExpression)){
      throw new RuntimeException("Assertion error"+" "+number.getClass());
    }
    ((LvalExpression)number).addReference();

    outputLine = Program.getLineNumber();
    assignments.add(this);
  }

  public int getOutputLine() {
    return outputLine;
  }
  
  public String toString(){
    return getOutputLine()+" "+CBuiltinFunctions.ASSERT_ZERO_NAME+" "+number;
  }
  
  public void toCircuit(Object obj, PrintWriter circuit) {
    circuit.print(getOutputLine()+" "+CBuiltinFunctions.ASSERT_ZERO_NAME+" ");
    circuit.print("inputs [ ");
    ((OutputWriter)number).toCircuit(null, circuit);
    circuit.println(" ]\t//void");
  }
}