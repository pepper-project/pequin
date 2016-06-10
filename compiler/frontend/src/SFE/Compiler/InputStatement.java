// InputStatement.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.io.PrintWriter;
import java.util.List;


/**
 * A class for representing input statement for the final output circuit.
 */
public class InputStatement extends StatementWithOutputLine implements OutputWriter, Optimizable {
  //~ Instance fields --------------------------------------------------------

  // data members

  /*
   * Holds the LHS of the assignment.
   */
  private LvalExpression input;
  

  /*
   * The number of this assignment statement line in the ouput circuit.
   */
  private int outputLine;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a new InputStatment from a given statement name and
   * input.
   * @param input the input LvalExpression stored in this InputStatment.
   */
  public InputStatement(LvalExpression input) {
    this.input = input;
    this.input.setAssigningStatement(this);
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return "input " + input + "\n";
  }

  /**
   * Returns a string representation of this InputStatement's name.
   * @return a string representation of this InputStatement's name.
   */
  public String getName() {
    return input.getName();
  }

  /**
   * Returns the output line in the output circuit of this assignmnet statement.
   * @return an int that represents the output line in the output circuit of this assignmnet statement.
   */
  public int getOutputLine() {
    return outputLine;
  }

  /**
   * Sets the output line of this assignment statement.
   * @param line the line number in the output.
   */
  public void setOutputLine(int line) {
    outputLine = line;
  }

  /**
   *  Prints this AssignmentStatement into the circuit.
   *  @param circuit the circuit output file.
   */
  public void toCircuit(Object obj, PrintWriter circuit) {
    circuit.print(outputLine + " input\t\t//" + input.getName());

    circuit.print(" "+input.getType().toString());
    circuit.println();
  }

  /**
   * Optimizes the InputStatement - phase I
   */
  public void optimize(Optimization job) {
    switch(job) {
    case RENUMBER_ASSIGNMENTS:
      //Get the next available number in the program.
      outputLine = Program.getLineNumber();
      break;
    case PEEPHOLE:
      //Done.
      break;
    default:
      //It's potentially dangerous if we perform an optimization and only some parts of our system implement it.
      //Catch that.
      throw new RuntimeException("Optimization not implemented: "+job);
    }
  }

  /**
   * Optimizes the InputStatement - phase II
   */
  public void blockOptimize(BlockOptimization job, List newBody) {
    switch(job) {
    case DEADCODE_ELIMINATE:
      newBody.add(this);
      break;
    default:
      //It's potentially dangerous if we perform an optimization and only some parts of our system implement it.
      //Catch that.
      throw new RuntimeException("Optimization not implemented: "+job);
    }
  }

  /**
   * adds this input statement to the statements being used to calculate
   * the output circuit.
   */
  public void buildUsedStatementsHash() {
    Optimizer.putUsedStatement(this);
  }

  /**
   * Transforms this multibit InputStatement into singlebit statements
   * and returns the result.
   * @param obj not needed (null).
   * @return a BlockStatement containing the result statements.
   */
  public BlockStatement toSLPTCircuit(Object obj) {
    BlockStatement result = new BlockStatement();

    //Handle the case where input is a compound type.
    for (int i = 0; i < input.size(); i++) {
      InputStatement is = new InputStatement(input.lvalFieldEltAt(i));
      result.addStatement(is);

			// assert(is.input.getLvalue().hasAddress());
			// int address = is.input.getLvalue().getAddress();
			//
			// Statement ramput = new
			// RamPutEnhancedStatement(IntConstant.valueOf(address), is.input);
			// ramput = ramput.toSLPTCircuit(null);
			// result.addStatement(ramput);
    }

    return result;
  }
  /**
   * Unique vars transformations.
  public Statement uniqueVars() {
  	//Get the next available number in the program.
  	outputLine = Program.getLineNumber();
  	return this;
  }
   */

  public void toAssignmentStatements(StatementBuffer assignments) {
    //We can set the input to the type it needs to be.
    outputLine = Program.getLineNumber();
    assignments.add(this);

    if (Function.getVar(input) != null) { //CompiledStatement does not use the VariableLUT system.
      Statement oldAssignment = Function.getVar(input).getAssigningStatement();
      if (oldAssignment != this) {
        Function.addVar(input); //.getName(), lhs.getType(), lhs.isOutput());
        // get the new ref to lhs
        input = Function.getVar(input);
    
        input.setAssigningStatement(this);
      }
    }
  }

  /**
   * dammy - returns this.
   */
  public Statement duplicate() {
    return this;
  }
}
