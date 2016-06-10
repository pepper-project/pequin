// Multi2SingleBit.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

/**
 * The circuit reduction takes a general circuit of complex gates
 * and produces a circuit which is a single level circuit, and in which
 * all gates are over primitive types (i.e. not over arrays or structs)
 *
 * In the original fairplay compiler, a primitive type was a boolean.
 * In our implementation, a primitive type is the float type.
 *
 * The output is a Single Level Primitive Typed circuit
 */
public interface SLPTReduction {
  //~ Methods ----------------------------------------------------------------

  /**
   * This method performs the transformation itself and add the result
   * statements to the appropriate function.
   * The object parameter is one parameter that is needed for each
   * class to implement this method and the parameter's role can vary
   * from one class to another.
   * If obj is not needed the method will be
   * called be the parameter obj as null.
   * @param obj the method's parameter.
   * @return a statement containing the statements as single bits.
   */
  public abstract Statement toSLPTCircuit(Object obj);
}
