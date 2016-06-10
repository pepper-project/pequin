// IntType.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

/**
 * A class representing the a signed integer of a certain number of bits.
 *
 * We require that n >= 1.
 *
 * So, it describes the set {-2^(n-1), -2^(n-1) + 1, ... 2^(n-1) - 1}.
 */
public class RestrictedSignedIntType extends RestrictedIntType{
  //~ Instance fields --------------------------------------------------------

  /*
   * Holds the length of this Int type, >= 1.
   */
  private final int length;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a RestrictedIntType object of a given length.
   */
  public RestrictedSignedIntType(int length) {
    super(new IntConstantInterval(IntConstant.negExp2(length-1), IntConstant.posExp2min1(length-1)));
    if (length <= 0) {
      throw new RuntimeException("Signed integer of length "+length+" not defined");
    }

    this.length = length;
  }
  public RestrictedSignedIntType(IntConstantInterval ici){
    super(ici);
    
    this.length = Math.max(getNeededLength(ici.lower),getNeededLength(ici.upper));
  }

  //~ Methods ----------------------------------------------------------------
  public static int getNeededLength(IntConstant a) {
    //extends from -2^(N-1) to 2^(N-1)-1
    return a.bitLength() + 1;
  }
  public int getLength() {
    return length;
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return "int bits " + length;
  }
}
