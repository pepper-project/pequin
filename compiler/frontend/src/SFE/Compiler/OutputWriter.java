// OutputWriter.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.io.PrintWriter;


/**
 * The OutputWriter represents the classes that produce the compiler's output.
 */
public interface OutputWriter {
  //~ Methods ----------------------------------------------------------------

  /**
   * Prints this Object into the circuit.
   * @param circuit the circuit output file.
   *
   * Object obj is an optional parameter.
   */
  abstract void toCircuit(Object obj, PrintWriter circuit);
}
