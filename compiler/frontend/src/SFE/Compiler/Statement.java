// Statement.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.io.PrintWriter;



/**
 * Abstract class for representing statements that can be defined
 * in the program.
 */
public abstract class Statement implements SLPTReduction {
  //~ Methods ----------------------------------------------------------------

  // data members

  /**
   * returns a replica of this statement.
   * @return a replica of this statement.
   */
  public abstract Statement duplicate();

  /**
   * The statement should be converted into assignment statements, which should be added
   * in sequence to the input collection.
   */
  public abstract void toAssignmentStatements(StatementBuffer assignments);
}
