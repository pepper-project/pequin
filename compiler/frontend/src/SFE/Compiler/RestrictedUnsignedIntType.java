// IntType.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;


/**
 * A class representing an unsigned integer of a certain number of bits.
 *
 * We require that n >= 1.
 *
 * So, it describes the set {0, 1, ... 2^n - 1}.
 */
public class RestrictedUnsignedIntType extends RestrictedIntType {
  //~ Instance fields --------------------------------------------------------

  /*
   * Holds the length of this Int type, >= 1.
   */
  private final int length;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a RestrictedIntType object of a given length.
   * 
   * When length == 0, it is interpreted as length = 1.
   */
  public RestrictedUnsignedIntType(int length) {
    this(lengthProcess(length),false);
  }
  private static int lengthProcess(int length) {
    if (length < 0){
      throw new RuntimeException("I don't know how to construct an unsigned int type with "+length+" bits.");
    }
    if (length == 0) {
      length = 1;
    }
    return length;
  }
  private RestrictedUnsignedIntType(int length, boolean dummy){
    super(new IntConstantInterval(IntConstant.ZERO, IntConstant.posExp2min1(length)));
    this.length = length;
  }
  public RestrictedUnsignedIntType(IntConstantInterval ici){
    super(ici);
    if (ici.lower.compareTo(IntConstant.ZERO) < 0){
      throw new RuntimeException("Assertion error: Negative lower bound on unsigned integer type");
    }
    
    this.length = getNeededLength(ici.upper);
  }


  //~ Methods ----------------------------------------------------------------

  private int getNeededLength(IntConstant a) {
    //extends from 0 to 2^(N)-1
    if (a.equals(IntConstant.ZERO)){
      return 1;
    }
    return a.bitLength();
  }
  public int getLength() {
    return length;
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return "uint bits " + length;
  }
}
