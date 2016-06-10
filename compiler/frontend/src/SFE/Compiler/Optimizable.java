// Optimize.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.List;


/**
 * Generic optimizable type. Some types will also implement OptimizeStatement.
 */
interface Optimizable {
  public static enum Optimization {
    RENUMBER_ASSIGNMENTS, //Give assignments numbers (needed for performing fast hashtable lookups of assignments)
    //The following all depend on RENUMBER_ASSIGNMENTS being called first
    PEEPHOLE,
    //SIMPLIFY,
    DUPLICATED_IN_FUNCTION, //Optimize given the information that these statements have just been duplicated inside a function
  }
  public static enum BlockOptimization {
    DEADCODE_ELIMINATE, //Dependencies: The RENUMBER_ASSIGNMENTS job (see Optimize)
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Perform the specified optimization job to transform the circuit
   * (essentially, run the operation over the computation tree to form a new computation tree recursively)
   * @param job
   */
  public void optimize(Optimization job);


  /**
   * Perform the specified optimization job to transform this statement
   * (essentially, run the operation over the computation tree to form a new computation tree recursively)
   * @param job
   */
  public void blockOptimize(BlockOptimization job, List body);

  /**
   * Used during DEADCODE_ELIMINATE
   */
  public void buildUsedStatementsHash();
}
