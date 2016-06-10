// BooleanConstant.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.io.PrintWriter;


/**
 * The BooleanConstant class represents boolean consts expressions that can
 * appear in the program.
 */
public class BooleanConstant extends ConstExpression implements OutputWriter {
  //~ Instance fields --------------------------------------------------------

  // data members

  /*
   * Holds the boolean constant of this Boolean constant
   */
  private boolean booleanConst;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a new BooleanConstant from a given boolean const
   * @param booleanConst the given boolean constant
   */
  public BooleanConstant(boolean booleanConst) {
    this.booleanConst = booleanConst;
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Returns 1, as the number of bits needed to represent this
   * BooleanConstant expression.
   * @return 1, as the number of bits needed to represent this
   * BooleanConstant expression.
   */
  public int size() {
    return 1;
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return "C" + ((booleanConst) ? "1" : "0");
  }

  /**
   * Returns the value stored in this BooleanConstant (0 or 1)
   * @return the value stored in this BooleanConstant (0 or 1)
   */
  public int value() {
    return (booleanConst) ? 1 : 0;
  }

  /**
   * Returns the boolean stored in this BooleanConstant
   * @return the boolean stored in this BooleanConstant
   */
  public boolean getConst() {
    return booleanConst;
  }

  public Type getType() {
    return new BooleanType();
  }

  /**
   * Returns Expression that represents the bit at place i of this Expression.
   * Note: i can only be 0.
   * @return Expression that represents the bit at place i of this Expression
   */
  public ConstExpression fieldEltAt(int i) {
    return this;
  }

  /**
   * Writes this constant into the circuit file.
   * @param circuit the output circuit.
   */
  public void toCircuit(Object obj, PrintWriter circuit) {
    circuit.print("C" + ((booleanConst) ? "1" : "0"));
  }

  //Static fields
  public static BooleanConstant TRUE = new BooleanConstant(true);
  public static BooleanConstant FALSE = new BooleanConstant(false);

  public static Expression valueOf(boolean bit) {
    if (bit) {
      return TRUE;
    } else {
      return FALSE;
    }
  }

  public static BooleanConstant toBooleanConstant(Expression oldCond) {
    FloatConstant fc = FloatConstant.toFloatConstant(oldCond);
    if (fc == null) {
      return null;
    }
    if (fc.isOne()) {
      return TRUE;
    }
    if (fc.isZero()) {
      return FALSE;
    }
    return null;
  }
}
