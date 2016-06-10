
// IntConstant.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.HashMap;


/**
 * The IntConstant class represents integer consts expressions that can
 * appear in the program.
 */
public class IntConstant extends ConstExpression implements Comparable<IntConstant>, OutputWriter {
  //~ Instance fields --------------------------------------------------------

  // data members

  /*
   * Holds the integer constant of this IntConstant
   */
  private final BigInteger intConst;


  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a new IntConstant from a given integer const
   * @param intConst the given integer constant
   */
  public IntConstant(BigInteger intConst) {
    this.intConst = intConst;
  }
  public IntConstant(int i){
    this(BigInteger.valueOf(i));
  }
  
  private static HashMap<Integer, IntConstant> PosExp2Min1 = new HashMap(); 
  /**
   * Returns an int constant with value (2^N) - 1 
   */
  public static IntConstant posExp2min1(int N) {
    IntConstant got = PosExp2Min1.get(N);
    if (got != null){
      return got;
    }
    BigInteger num = BigInteger.ONE;
    num = num.shiftLeft(N);
    num = num.subtract(BigInteger.ONE);
    IntConstant neu = IntConstant.valueOf(num);
    PosExp2Min1.put(N, neu);
    return neu;
  }
  private static HashMap<Integer, IntConstant> NegExp2 = new HashMap(); 
  /**
   * Returns an int constant with value -(2^N) 
   */
  public static IntConstant negExp2(int N) {
    IntConstant got = NegExp2.get(N);
    if (got != null){
      return got;
    }
    BigInteger num = BigInteger.ONE;
    num = num.shiftLeft(N);
    num = num.negate();
    IntConstant neu = IntConstant.valueOf(num);
    NegExp2.put(N, neu);
    return neu;
  }

  public static IntConstant toIntConstant(Expression c) {
    if (c instanceof IntConstant) {
      return (IntConstant)c;
    }
    FloatConstant fc = FloatConstant.toFloatConstant(c);
    if (fc != null) {
      BigInteger divisible = fc.getNumerator().mod(fc.getDenominator());
      if (divisible.signum() == 0) {
        return IntConstant.valueOf(fc.getNumerator().divide(fc.getDenominator()));
      }
    }
    return null;
  }

  //~ Methods ----------------------------------------------------------------


  public Type getType() {
    if (equals(ONE) || equals(ZERO)) {
      return new BooleanType();
    }
    IntConstantInterval interval = new IntConstantInterval(this, this);
    switch(intConst.signum()){
    case 0:
      throw new AssertionError();
    case 1:
      return new RestrictedUnsignedIntType(interval);
    case -1:
      return new RestrictedSignedIntType(interval);
    default: 
      throw new AssertionError();
    }
  }
  
  /**
   * Returns intConst.bitLength()
   */
  public int bitLength(){
    return intConst.bitLength();
  }
  
  
  public boolean equals(Object o){
    return ((IntConstant)o).intConst.equals(intConst);
  }

  /**
   * Since float is our primitive type, this returns 1.
   */
  public int size() {
    return 1;
  }



  /**
   * Returns the value stored in this IntConstant
   * @return the value stored in this IntConstant
   */
  public BigInteger value() {
    return intConst;
  }

  /**
   * Returns this (because an int is a primitive value)
   */
  public ConstExpression fieldEltAt(int i) {
    return this;
  }

  //~ Static fields/initializers ---------------------------------------------

  /**
   * Writes this constant into the circuit file.
   * @param circuit the output circuit.
   */
  public void toCircuit(Object obj, PrintWriter circuit) {
    circuit.print("C"+intConst);
  }
  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return "C"+intConst.toString(10);
  }

  public IntConstant add(IntConstant other){
    return IntConstant.valueOf(intConst.add(other.intConst));
  }
  public IntConstant multiply(IntConstant other){
    return IntConstant.valueOf(intConst.multiply(other.intConst));
  }
  
  public int compareTo(IntConstant other) {
    return intConst.compareTo(other.intConst);
  }

  //Public static fields
  public static final IntConstant ONE = new IntConstant(1);
  public static final IntConstant NEG_ONE = new IntConstant(-1);
  public static final IntConstant ZERO = new IntConstant(0);


  /**
   * Non-type safe valueOf. Argument must either be an Integer or a BigInteger.
   */
  public static IntConstant valueOf(Number i_) {
    BigInteger i;
    if (i_ instanceof Integer){
      i = BigInteger.valueOf((Integer)i_); 
    } else {
      i = (BigInteger)i_;
    }
    for(IntConstant q : new IntConstant[]{ONE, NEG_ONE, ZERO}){
      if (i.equals(q.intConst)){
        return q;
      }
    }    
    return new IntConstant(i);
  }

  public boolean isPOT() {
    return intConst.signum() > 0 && (intConst.bitCount() == 1);
  }
  public int signum() {
    return intConst.signum();
  }
  public int toInt() {
    BigInteger MIN_INT = BigInteger.valueOf(Integer.MIN_VALUE);
    BigInteger MAX_INT = BigInteger.valueOf(Integer.MAX_VALUE);
    if (intConst.compareTo(MIN_INT) < 0 || intConst.compareTo(MAX_INT) > 0){
      throw new RuntimeException("Cannot coerce safely to int: "+intConst);
    }
    return intConst.intValue();
  }
}
