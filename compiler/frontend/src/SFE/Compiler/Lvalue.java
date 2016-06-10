// Lvalue.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.Vector;


/**
 * Abstract class that Defines an entity that can appear on the LHS of an
 * assignment.
 */
public abstract class Lvalue {
  //~ Instance fields --------------------------------------------------------

  /*
   * true if this variable is output
   */
  protected boolean isOutput;

  public Lvalue () {

  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Returns true is the variable is a part out the circuit's output.
   * @return true is the variable is a part out the circuit's output.
   */
  public boolean isOutput() {
    return isOutput;
  }

  /**
   * sets this lvalue as a pin that is exported outsite this circuit.
   */
  public void notOutput() {
    isOutput = false;
  }

  /**
   * Returns the Type of this Lvalue object.
   * @return the Type of this lvalue object.
   */
  public abstract Type getType();


  /**
   * Sets the Type of this Lvalue object.
   */
  public abstract void setType(Type t);

  /**
   * Returns the name of the lvalue of this object.
   * @return a string representing this lvalue's name.
   */
  public abstract String getName();
  
  public abstract boolean hasAddress();

  public abstract int getAddress();
  
  public abstract void allocateStackAddress();
  public abstract void allocateHeapAddress();

  /**
   * Returns a vector of all the derived lvalue of this type.
   * type that should return more then one lvalue (derived from the type itself)
   * in the vector must overide this method.
   * @return a vector of all the derived lvalue of this type.
   */
  public Vector getDerivedLvalues() {
    return getType().getDerivedLvalues(this);
  }

  /**
   * Returns true/
   */
  public boolean isPrimitive() {
    //Primitive elements are those that do not
    return getType().fieldEltAt("", 0).equals("");
  } 
}
