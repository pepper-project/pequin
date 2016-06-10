// VarLvalue.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;


/**
 * The VarLvalue class extends the Lvalue class, and can be
 * when a variable is used as Lvalue.
 */
public class VarLvalue extends Lvalue {
  //~ Instance fields --------------------------------------------------------

  //data members
  protected Variable variable;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a new VarLvalue from a given variable.
   * @param variable the variable from which to construct this VarLvalue.
   */
  public VarLvalue(Variable variable, boolean isOutput) {
    this.variable     = variable;
    this.isOutput     = isOutput;
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Returns the Type of this Lvalue object.
   * @return the Type of this lvalue object.
   */
  public Type getType() {
    return variable.getType();
  }

  /**
   * Returns the declared type of this lvalue's variable
   */
  public Type getDeclaredType() {
    return variable.getDeclaredType();
  }


  public void setType(Type t) {
    variable.setType(t);
  }

  /**
   * Returns the size of this Lvalue object in bits.
   * @return an integer representing size of this lvalue object in bits.
   */
  public int size() {
    return variable.size();
  }

  /**
   * Returns the name of the lvalue of this object.
   * @return a string representing this lvalue's name.
   */
  public String getName() {
    return variable.getName();
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return variable.getName();
  }

  public boolean hasAddress() {
  	return variable.hasAddress();
  }
  
	@Override
  public int getAddress() {
	  return variable.getAddress();
  }

	@Override
  public void allocateStackAddress() {
	  variable.allocateStackAddress();
  }

	@Override
  public void allocateHeapAddress() {
		variable.allocateHeapAddress();
  }
}
