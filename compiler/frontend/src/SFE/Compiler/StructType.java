// StructType.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.Vector;


/**
 * Class StructType is used for representing a Struct that was defined in
 * the program. An object of StructType contains a vector containing the field
 * that were defined a the data member of this StructType.
 */
public class StructType extends ParentType {
  //~ Instance fields --------------------------------------------------------

  // data members

  /*
   * Holds the fields of this struct
   */
  private Vector<String> fieldsName;
  private Vector<Type> fieldsType;

  /*
   * Holds the size of this StructType
   */
  private int size;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a new empty StructType object.
   */
  public StructType() {
    size           = 0;
    fieldsName     = new Vector();
    fieldsType     = new Vector();
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Returns the length of the this Struct type in bits.
   * @return the length of the this Struct type in bits.
   */
  public int size() {
    return size;
  }

  /**
   * Adds a new field as a data member to this StructType.
   * @param fieldName the name of the new field.
   * @param type the type of the new field.
   */
  public void addField(String fieldName, Type type) {
    size += type.size();

    fieldsName.add(fieldName);
    fieldsType.add(type);
  }

  /**
   * Returns a string representation of the this struct type.
   * @return the string "struct" as the string string representation of this struct type.
   */
  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append("struct {");
    for(int i = 0; i < fieldsName.size(); i++){
      sb.append(fieldsType.get(i).toString());
      sb.append(" ");
      sb.append(fieldsName.get(i));
      sb.append(";");
    }
    sb.append("}");
    return sb.toString();
  }

  /**
   * Return the object representing the type of the specified field name, or null if there was no
   * such field defined for this StructType.
   * @param fieldName the field name whose associated type is to be returned.
   * @return the object representing the type of the specified field name, or null if there was no
   * such field defined for this StructType.
   */
  public Type fromFieldName(String fieldName) {
    for (int i = 0; i < fieldsName.size(); i++)
      if (((String) fieldsName.elementAt(i)).equals(fieldName)) {
        return (Type) (fieldsType.elementAt(i));
      }

    return null;
  }

  /**
   * Returns a String representing this object as it should appear in the format file.
   * @return a String representing this object as it should appear in the format file.
   */
  public String toFormat(String parentName, Function function) {
    throw new RuntimeException("Not yet implemented");

    /*
    String str = new String();

    for (int f = 0; f < fieldsName.size(); f++) {
    	String name = (String) fieldsName.elementAt(f);
    	Type   type = (Type) fieldsType.elementAt(f);

    	if (type instanceof StructType) {
    		str += ((StructType) type).toFormat(parentName + "." + name,
    		                                    function);
    	} else if (type instanceof ArrayType) {
    		str += ((ArrayType) type).toFormat(parentName + "." + name,
    		                                   function);
    	} else {
    		// get input/ouput and alice/bob
    		String params   = parentName + "." + name;
    		String alicebob;

    		if (params.startsWith("output.alice")) {
    			alicebob = "Alice";
    		} else {
    			alicebob = "Bob";
    		}

    		// <Alice|Bob> <Input|Ouput> <type> <prompt(field name)> <'[' input bits ']'>
    		str += (alicebob + " output " + type.toFormat() + " \"" +
    		params + "\" [ ");

    		for (int i = 0; i < type.size(); i++) {
    			AssignmentStatement s =
    				(AssignmentStatement) (Function.getVar(parentName +
    				                                       "." + name +
    				                                       "$" + i)
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
   * Returns a list with all this struct's field names.
   * @return a list with all this struct's field names.
   */
  public Vector<String> getFields() {
    return fieldsName;
  }
  
  /**
   * Returns a list with all the struct's field types. 
   * Each index corresponds to an index in the getFields() array. 
   */
  public Vector<Type> getFieldTypes(){
    return fieldsType;
  }

  /**
   * DAMMY Returns a String representing this object as it should appear in the format file.
   * @return a String representing this object as it should appear in the format file.
   */
  public String toFormat() {
    return "";
  }

  /**
   * Returns the name of the bit at offset i in the struct.
   * @return the name of the bit at offset i in the struct.
   */
  public String fieldEltAt(String baseName, int i) {
    int i_old = i;
    if (i < 0) {
      throw new ArrayIndexOutOfBoundsException("Index negative "+i+" into struct fieldEltAt "+baseName);
    }

    for (int f = 0; f < fieldsName.size(); f++) {
      String name = (String) fieldsName.elementAt(f);
      Type   type = (Type) fieldsType.elementAt(f);

      // get to the next field
      if ((type.size() <= i) && (f < (fieldsName.size() - 1))) {
        i -= type.size();

        continue;
      }

      // (else)
      if ((type.size() <= i) && ! (f < (fieldsName.size() - 1))) {
        // sign expantion -  type.size() < i and last field
        throw new RuntimeException("Array index out of bounds exception index "+i_old+" in struct fieldEltAt "+baseName);
        //i = type.size() - 1;
      }

      return type.fieldEltAt(baseName + "." + name, i);
    }

    return null; //dammy
  }

  /**
   * Returns a vector of all the derived lvalue (inluding this lvalue).
   * lvalue that shold return more then this in the vector must overide this
   * method.
   * @param base the lavalue that call the this method (base.type == this)
   * @return Vector of all the lvalues
   */
  public Vector getDerivedLvalues(Lvalue base) {
    Vector result = new Vector();
    result.add(base);

    for (int fieldIndex = 0; fieldIndex < fieldsName.size();
         fieldIndex++) {
      // get field name and Type
      String fieldName = (String) fieldsName.elementAt(fieldIndex);
      Type   fieldType = (Type) fieldsType.elementAt(fieldIndex);

      StructFieldLvalue structFieldLval =
        new StructFieldLvalue(base, fieldName);
      result.addAll(fieldType.getDerivedLvalues(structFieldLval));
    }

    return result;
  }

  public Type fieldEltTypeAt(int i) {
    int i_old = i;
    
    for (int f = 0; f < fieldsName.size(); f++) {
      String name = (String) fieldsName.elementAt(f);
      Type   type = (Type) fieldsType.elementAt(f);

      // get to the next field
      if ((type.size() <= i) && (f < (fieldsName.size() - 1))) {
        i -= type.size();

        continue;
      }

      // (else)
      if ((type.size() <= i) && ! (f < (fieldsName.size() - 1))) {
        // sign expantion -  type.size() < i and last field
        throw new RuntimeException("Array index out of bounds exception index "+i_old+" in struct fieldEltTypeAt "+this);
        //i = type.size() - 1;
      }

      return type.fieldEltTypeAt(i);
    }

    throw new RuntimeException("Array index out of bounds exception index "+i_old+" in struct fieldEltTypeAt "+this);
  }
}
