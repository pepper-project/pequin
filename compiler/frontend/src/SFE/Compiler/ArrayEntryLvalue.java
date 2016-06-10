// ArrayEntryLvalue.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

/**
 * ArrayEntryLvalue represent as array entry l-value that can be defined
 * in a program. The ArrayEntryLvalue class extends the VarLvalue class.
 */
public class ArrayEntryLvalue extends VarLvalue {
  //~ Instance fields --------------------------------------------------------

  //data members

  /*
   * The array Lvalue (lvalue has ArrayType type)
   */
  private Lvalue array;

  /*
   * The offset int the array.
   */
  private int index;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a new ArrayEntryLvalue from a given lvalue and index
   * in the array.
   * @param array the array's l-value.
   * @param index the index of this ArrayEntryLvalue in the array.
   */
  public ArrayEntryLvalue(Lvalue array, int index) {
    super(new Variable(array.getName()+"["+index+"]",((ArrayType)array.getType()).getComponentType()), array.isOutput());
    this.array        = array;
    this.index        = index;
    //this.isOutput     = array.isOutput();
  }
}
