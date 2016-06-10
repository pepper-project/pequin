// IntType.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.math.BigInteger;

/**
 * A class representing the integer primitive type of a
 */
public class IntType extends ScalarType {
  //~ Instance fields --------------------------------------------------------

  // data members

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs an IntType object.
   */
  public IntType() {
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return "int";
  }
  
  public static int getBits(IntType c) {
    if (c instanceof RestrictedUnsignedIntType){
      return ((RestrictedUnsignedIntType)c).getLength();
    } else if (c instanceof RestrictedSignedIntType){
      return ((RestrictedSignedIntType)c).getLength();
    }
    throw new RuntimeException("I don't know how to get the number of bits in type "+c);
  }

  public static BigInteger getMaxInt(IntType c) {
    if (c instanceof RestrictedSignedIntType){
      return BigInteger.ONE.shiftLeft(getBits(c)-1).subtract(BigInteger.ONE);
    } else {
      return BigInteger.ONE.shiftLeft(getBits(c)).subtract(BigInteger.ONE);
    }
  }

  public static BigInteger getMinInt(IntType c) {
    if (c instanceof RestrictedSignedIntType){
      return BigInteger.ONE.shiftLeft(getBits(c)-1).negate();
    } else {
      return BigInteger.ZERO;
    }
  }
}
