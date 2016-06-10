// ArraType.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.Vector;


/**
 * Class ArraType is used for representing an Array that was defined in
 * the program.
 */
public class ArrayType extends ParentType {
  //~ Instance fields --------------------------------------------------------

  // data members

  /*
   * Holds the array length
   */
  private int length;

  /*
   * the base type of the array
   */
  private Type baseType;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a new ArrayType object from a given length and
   * base type.
   * @param type the base type of this array.
   * @param length the length of this array.
   */
  public ArrayType(Type baseType, int length) {
    this.length       = length;
    this.baseType     = baseType;
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Returns the number of field elements used to represent an instance of this arraytype
   */
  public int size() {
    return length * baseType.size();
  }

  /**
   * Returns the number of components in this array type
   */
  public int getLength() {
    return length;
  }

  /**
   * Returns a string representation of the this ArrayType.
   * @return the string "array" as the string representation
   *                 of this ArrayType.
   */
  public String toString() {
    return "("+baseType.toString()+")["+getLength()+"]";
  }

  /**
   * Returns a String representing this object as it should
   * appear in the format file.
   */
  public String toFormat() {
    throw new RuntimeException("Not yet implemented");
  }

  /**
   * Returns the type of the elements in this array type
   */
  public Type getComponentType() {
    return baseType;
  }

  /**
   * Returns a vector of all the derived lvalue (inluding this lvalue).
   * Basically, the vector will hold this lvalue and all the
   * entries lvalue.
   * @param base the lvalue that called the this method (base.type == this)
   */
  public Vector getDerivedLvalues(Lvalue base) {
    Vector result = new Vector();
    result.add(base);

    //Place holder for indeterminate array element
    String    arrayTop = base.getName() + "[$]";
    VarLvalue top = new VarLvalue(new Variable(arrayTop, baseType), base.isOutput());
    result.addAll(baseType.getDerivedLvalues(top)); //recursively add elements

    for (int i = 0; i < length; i++) {
      ArrayEntryLvalue element = new ArrayEntryLvalue(base, i);
      //String    elementName = base.getName() + "["+i+"]";
      //VarLvalue element = new VarLvalue(new Variable(elementName, baseType), base.isOutput());
      result.addAll(baseType.getDerivedLvalues(element)); //recursively add elements
    }

    return result;
  }

  /*
  public Vector<String> getDerivedCvalues(String baseName) {
    Vector result = new Vector();
    result.add(baseName);

    //Place holder for indeterminate array element
    final String arrayTop = baseName + "[$]";
    result.addAll(baseType.getDerivedCvalues(arrayTop));

    for (int i = 0; i < length; i++) {
      result.addAll(.getDerivedCvalues(baseName+"["+i+"]"));
    }

    return result;
  }
  */

  /**
   * Returns a String representing this object as it should appear in
   * the format file.
   * @return a String representing this object as it should appear
   * in the format file.
   */
  public String toFormat(String parentName, Function function) {
    throw new RuntimeException("Not yet implemented");
    /*
    String str = new String();

    for (int i = 0; i < length; i++) {
    	if (baseType instanceof StructType) {
    		str += ((StructType) baseType).toFormat(parentName + "[" + i +
    		                                        "]", function);
    	} else if (baseType instanceof ArrayType) {
    		str += ((ArrayType) baseType).toFormat(parentName + "[" + i +
    		                                       "]", function);
    	} else {
    		// get input/ouput and alice/bob
    		String params   = parentName + "[" + i + "]";
    		String alicebob;

    		if (params.startsWith("output.alice")) {
    			alicebob = "Alice";
    		} else {
    			alicebob = "Bob";
    		}

    		// <Alice|Bob> <Input|Ouput> <type> <prompt(field name)>
    		// <'[' input bits ']'>
    		str += (alicebob + " output " + baseType.toFormat() + " \"" +
    		params + "\" [ ");

    		for (int j = 0; j < baseType.size(); j++) {
    			AssignmentStatement s =
    				(AssignmentStatement) (Function.getVar(parentName +
    				                                       "[" + i + "]$" +
    				                                       j)
    				                               .getAssigningStatement());
    			str += (s.getOutputLine() + " ");
    		}

    		str += "]\n";
    	}
    }


    return str;
    */
  }

  /**
   * Returns the name of the ith field element in the representation of this type
   */
  public String fieldEltAt(String baseName, int i) {
    int baseSize = baseType.size();
    int entry;
    int offset;

    if (i < 0) {
      throw new ArrayIndexOutOfBoundsException("Index negative "+i+" in array "+baseName);
    }

    if (i < (baseSize * length)) {
      entry      = i / baseSize;
      offset     = i % baseSize;
    } else { 
      throw new RuntimeException("Array index out of bounds "+i+" in array "+baseName);
    }

    return baseType.fieldEltAt(baseName + "[" + entry + "]", offset);
  }

  public Type fieldEltTypeAt(int i) {
    int baseSize = baseType.size();
    int entry;
    int offset;

    if (i < 0) {
      throw new ArrayIndexOutOfBoundsException("Index negative "+i+" in array type " +this);
    }

    if (i < (baseSize * length)) {
      entry      = i / baseSize;
      offset     = i % baseSize;
    } else { 
      throw new RuntimeException("Array index out of bounds "+i+" in array type " +this);
    }
    return baseType.fieldEltTypeAt(offset);
  }

  public Type intersect(Type other) {
    throw new RuntimeException("Not implemented");
  }

  public boolean isSubTypeOf(Type other) {
    throw new RuntimeException("Not implemented");
  }
}
