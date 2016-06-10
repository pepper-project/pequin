// Program.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.HashMap;

import SFE.Compiler.Operators.NotEqualOperator;
import SFE.Compiler.Operators.UnaryPlusOperator;
import SFE.Compiler.Optimizable.BlockOptimization;
import SFE.Compiler.Optimizable.Optimization;


/**
 * A class that represents the program. It holds all the inforamtion needed
 * for the program.
 */
public class Program {
  //~ Instance fields --------------------------------------------------------

  private String name;
  private int tailoredQCQs = 0; // #of qcq's to use, in a tailored scheme. 0 = untailored (uses 1 qcq).

  //~ Constructors -----------------------------------------------------------
  public Program() {
    functions     = new HashMap<String, Function>();

    resetCounter(0);
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Adds a new function to this program.
   * @param function the new function to add.
   */
  public void addFunction(Function function) {
    Object replacing = functions.put(function.getName(), function);
    if (replacing != null) {
      throw new RuntimeException("Function declared twice: "+function.getName());
    }

    if (function.isOutput()) {
      if (Program.main != null) {
        throw new RuntimeException("Program can have at most one main method");
      }
      //The main function
      Program.main = function;
    }
  }

  public void setName(String name) {
    this.name = name;
  }
  public void setTailoringQCQs(int tailoredQCQs) {
    this.tailoredQCQs = tailoredQCQs;

    //Behavior of certain compilation features depends on whether we are tailoring or not.
    boolean tailored = tailoredQCQs > 0;
    if (tailored) {
      NotEqualOperator.setDualVariableInput(false);
    } else {
      NotEqualOperator.setDualVariableInput(true);
    }
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    String str = new String("program " + name + " {\n");

    str += main.toString();

    str += "}\n";

    return str;
  }

  /**
   * Remove struct and array operations. Add intermediate variables for multi-operator expressions.
   */
  public void toSLPTCircuit(Object obj) {
    Function.getVars().toSLPTCircuit();
    main.toSLPTCircuit(null);
  }


  /**
   * resets the line counter to have value start
   * @param start
   */
  public static void resetCounter(int start) {
    circuitLineCounter = start;
  }

  /**
   * Returns an integer identifying a line for assigning to the specified LValue.
   *
   * Some Lvalues will have prespecified output line numbers, and we want to preserve these.
   * The remaing LValues are simply given the next available line number.
   */
  public static int getLineNumber() {
    return circuitLineCounter++;
  }


  public void addInputStatements() {
    // add input statements
    main.addInputStatements();
  }

  public void addOutputStatements() {
    if (main.getFunctionResult() == null) {
      return;
    }

    //Add an assignment of output = output, to make sure that the final output lines are in the order
    //that the programmer expects (as opposed to in the order that the assignments are made in the code)
    main.addStatement(new AssignmentStatement(main.getFunctionResult(), new UnaryOpExpression(
                        new UnaryPlusOperator(), main.getFunctionResult())));
  }

  /**
   * Optimizes the circuit as it stands.
   *
   * Performs inlining, redundant code recognition, and dead code elimination.
   */
  public void optimize() {
    resetCounter(0);
    Optimizer.initOptimizer();

    System.out.println("Optimization started.");
    main.optimize(Optimization.RENUMBER_ASSIGNMENTS);

    System.out.println("Running peephole optimization");
    main.optimize(Optimization.PEEPHOLE);

    System.out.println("Eliminating dead code");
    main.blockOptimize(BlockOptimization.DEADCODE_ELIMINATE, null);

    //NOTE: the line numbers of assignments is still not final (toCircuit will probably change them)
    System.out.println("Optimization finished.");
  }

  /**
   * Unique vars transformations.
  public void uniqueVars() {
  	System.out.println("Unique vars transformations.");

  	resetCounter();
  	main.uniqueVars();
  	System.out.println("Unique vars transformations finished.");
  }
   */

  public static Function functionFromName(String name) {
    return functions.get(name);
  }

  //~ Static fields/initializers ---------------------------------------------

  /*
   * Holds the functions defined in the program
   */
  private static HashMap<String, Function> functions;

  /**
   * Hold a refernece to the main function
   */
  public static Function main;
  private static int     circuitLineCounter;

}
