package SFE.Compiler;

import java.io.PrintWriter;
import java.util.ArrayList;

import SFE.Compiler.Operators.ArrayAccessOperator;
import SFE.Compiler.Operators.PlusOperator;
import SFE.Compiler.Operators.PointerAccessOperator;
import SFE.Compiler.Operators.UnaryPlusOperator;

import ccomp.CBuiltinFunctions;

/**
 * A statement that causes the prover to execute a printf when it executes it.
 * It is not verified, and does not expand to any constraints.
 */
public class PrintfStatement extends StatementWithOutputLine implements OutputWriter{
  private int outputLine;
  private String formatString;
  private ArrayList<Expression> args;
  
  //First arg is a pointer to a string constant (format string)
  //remaining args are expressions to print with the format string
  public PrintfStatement(ArrayList<Expression> args) {
    formatString = parseFormatString(args.get(0));
    
    this.args = new ArrayList();
    for(int i = 1; i < args.size(); i++){
      this.args.add(args.get(i));
    }
  }

  public PrintfStatement(String formatString, ArrayList<Expression> args) {
    this.formatString = formatString;
    this.args = args;
  }

  private String parseFormatString(Expression expression) {
    StringBuffer built = new StringBuffer();
    int index = 0;
    while(true){
      char value = (char)IntConstant.toIntConstant(
            new UnaryOpExpression(new PointerAccessOperator(), 
                new BinaryOpExpression(new PlusOperator(), expression, IntConstant.valueOf(index++))
            )
          ).toInt();
      if (value == 0){
        break;
      }
      built.append(value);
    }
    return built.toString();
  }

  private static int uniquifier = 0;
  public Statement toSLPTCircuit(Object obj) {
    BlockStatement result = new BlockStatement();

    ArrayList<Expression> newArgs = new ArrayList();
    int argIndex = 0;
    for(Expression q : args){
      // create a temp var that holds the number
      Expression number_lval = q.evaluateExpression("Printf"+uniquifier++, ""+argIndex, result);
      newArgs.add(number_lval);
      argIndex++;
    }
    this.args = newArgs;

    result.addStatement(this);

    return result;
  }

  public Statement duplicate() {
    throw new RuntimeException("Not yet implemented");
  }

  public void toAssignmentStatements(StatementBuffer assignments) {
    //Change refs
    ArrayList<Expression> newArgs = new ArrayList();
    for(Expression q : args){
      newArgs.add(q.changeReference(Function.getVars()));
    }
    this.args = newArgs;
    
    toAssignmentStatements_NoChangeRef(assignments);
  }
  public void toAssignmentStatements_NoChangeRef(StatementBuffer assignments) {
    ArrayList<Expression> newArgs = new ArrayList();
    for(Expression number : args){
      LvalExpression asNum = LvalExpression.toLvalExpression(number);
      if (asNum == null){
        throw new RuntimeException("Assertion error "+number);
      }
      asNum.addReference();
      newArgs.add(asNum);
    }
    this.args = newArgs;

    outputLine = Program.getLineNumber();
    assignments.add(this);
  }

  public int getOutputLine() {
    return outputLine;
  }
  
  public String toString(){
    return getOutputLine()+" printf "+args;
  }
  
  public void toCircuit(Object obj, PrintWriter circuit) {
    circuit.print(getOutputLine()+" printf \"" + formatString+"\" ");
    circuit.print("inputs [ ");
    for(Expression number : args){
      ((OutputWriter)number).toCircuit(null, circuit);
      circuit.print(" ");
    }
    circuit.println(" ]\t//void");
  }
}