// Type.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.HashMap;
import java.util.Vector;


/**
 * Abstract class for representing types that can be defined
 * in the program. This class also functions as a type table for the
 * defined types in the programs.
 */
public abstract class Type {
  //~ Methods ----------------------------------------------------------------

  /**
   * Returns the object representing the type of the specified type name, or null if there was no
   * such type defined for this type name.
   * @param typeName the type name whose associated type is to be returned.
   * @return the object representing the type of the specified type name, or null if there was no
   * such type defined for this type name.
   */
  public static Type fromName(String typeName) {
    return (Type) (typeTable.get(typeName));
  }

  /**
   * Associates the specified newTypeName with the specified newType.
   * @param newTypeName the new Type name with which the specified Type is to be associated.
   * @param newType the Type to be associated with the specified newTypeName.
   * @throws IllegalArgumentException if the newTypeName is already defined.
   */
  public static void defineName(String newTypeName, Type newType)
  throws IllegalArgumentException {
    if (typeTable.containsValue(newTypeName)) {
      throw new IllegalArgumentException();
    }

    typeTable.put(newTypeName, newType);
  }

  /**
   * Returns a vector of all the derived lvalue of this type.
   * type that shold return more then one lvalue (derived from the type itself)
   * in the vector must overide this method.
   * @param base the lavalue that call the this method (base.type == this)
   */
  public Vector getDerivedLvalues(Lvalue base) {
    Vector result = new Vector();
    result.add(base);

    return result;
  }

  /**
   * Returns the number of field elements used to represent the result of this expression.
   */
  public abstract int size();
  
  /**
   * Returns the name of the ith field element used to represent an instance called "rootName"
   * of this type. The returned name should be one of the Lvalues returned
   * from a call to getDerivedLvalues(root);
   */
  public abstract String fieldEltAt(String rootName, int bit);

  /**
   * Returns a type describing the ith field element used to represent the result of this
   * expression
   */
  public abstract Type fieldEltTypeAt(int bit);
  
  //~ Static fields/initializers ---------------------------------------------

  // data members

  /*
   * holds the types defined in the program.
   */
  private static HashMap typeTable = new HashMap();
}
