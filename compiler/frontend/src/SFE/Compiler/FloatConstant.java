// IntConstant.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.io.PrintWriter;
import java.math.BigInteger;

/**
 * The IntConstant class represents integer consts expressions that can
 * appear in the program.
 */
public class FloatConstant extends ConstExpression implements OutputWriter, Comparable<FloatConstant> {
  //~ Instance fields --------------------------------------------------------

  // data members

  private BigInteger num;
  private BigInteger den;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a new IntConstant from a given integer const
   * @param intConst the given integer constant
   */
  private FloatConstant(BigInteger num, BigInteger den) {
    this.num = num;
    this.den = den;
    if (den.signum()<=0) {
      throw new ArithmeticException("Denominator of rational number is always positive");
    }
  }
  /*
  public FloatConstant(FloatConstant toCopy) {
  	this(toCopy.num, toCopy.den);
  }
  public FloatConstant(IntConstant num, IntConstant den) {
  	super(null);
  	this.num = num;
  	this.den = den;
  	if (den<=0){
  		throw new ArithmeticException("Denominator of rational number is always positive");
  	}
  }
  */
  /*
   * Note - intConstant defers to FloatConstant when parsing, so don't call intConstant's valueOf!
   */
  public static FloatConstant valueOf(String substring) {
    if (substring.startsWith("0x")){
      return valueOf(new BigInteger(substring.substring(2), 16), BigInteger.ONE);
    }
    String[] words = substring.split("/");
    BigInteger num = new BigInteger(words[0],10);
    BigInteger den;
    if (words.length > 1) {
      den = new BigInteger(words[1],10);
    } else {
      den = BigInteger.ONE;
    }
    return valueOf(num, den);
  }
  /**
   * Non-typesafe valueOf.
   * 
   * Both arguments must be either integers or BigIntegers.
   */
  public static FloatConstant valueOf(Number num_, Number den_) {
    BigInteger num;
    BigInteger den;
    if (num_ instanceof Integer){
      num = BigInteger.valueOf((Integer)num_);
    } else {
      num = (BigInteger) num_;
    }
    if (den_ instanceof Integer){
      den = BigInteger.valueOf((Integer)den_);
    } else {
      den = (BigInteger) den_;
    }
    
    FloatConstant copy = new FloatConstant(num, den);
    if (copy.isOne()) {
      return ONE;
    }
    if (copy.isZero()) {
      return ZERO;
    }
    if (copy.isNegOne()) {
      return NEG_ONE;
    }
    return copy;
  }
  /**
   * In performing this coercion, variables may be created and assignmentstatements may be formed.
   */
  public static FloatConstant toFloatConstant(Expression c) {
    if (c instanceof FloatConstant) {
      return (FloatConstant)c;
    }
    if (c instanceof IntConstant) {
      return FloatConstant.valueOf(((IntConstant) c).value(), BigInteger.ONE);
    }
    if (c instanceof BooleanConstant) {
      return FloatConstant.valueOf(BigInteger.valueOf(((BooleanConstant) c).value()), BigInteger.ONE);
    }

    if (c instanceof UnaryOpExpression) {
      UnaryOpExpression uo = ((UnaryOpExpression)c);
      return toFloatConstant(uo.getOperator().resolve(uo.getMiddle()));
    }
    if (c instanceof BinaryOpExpression) {
      BinaryOpExpression bo = ((BinaryOpExpression)c);
      return toFloatConstant(bo.getOperator().resolve(bo.getLeft(), bo.getRight()));
    }
    
    if (c instanceof PolynomialExpression){ 
      PolynomialExpression pe = (PolynomialExpression)c;
      Expression got = pe.inline(null, null);
      if (!(got instanceof PolynomialExpression)){//Ensure progress
        return toFloatConstant(got);
      }
    }
    
    AssignmentStatement as = AssignmentStatement.getAssignment(c);
    if (as != null){
      for(Expression q : as.getAllRHS()){
        FloatConstant got = toFloatConstant(q);
        if (got != null){
          return got;
        }
      }
    }
    /*
    AssignmentStatement as = AssignmentStatement.getAssignment(c);
    if (as != null){
      for(Expression r : as.getAllRHS()){
        if (r instanceof ConstExpression){
          return toFloatConstant(r);
        }
      }
    }
    */
    
    return null;
  }
  //~ Methods ----------------------------------------------------------------

  /**
   * Since float is our primitive type, this returns 1.
   */
  public int size() {
    return 1;
  }

  public BigInteger getNumerator() {
    return num;
  }
  public BigInteger getDenominator() {
    return den;
  }
  public boolean isZero() {
    return num.signum() == 0;
  }
  /**
   * Weak.
   */
  public boolean isOne() {
    return num.equals(den);
  }
  /**
   * Weak.
   */
  public boolean isNegOne() {
    return num.negate().equals(den);
  }
  /**
   * Returns -1, 0, or 1 depending on the comparison between this and arg0.
   */
  public int compareTo(FloatConstant arg0) {
    FloatConstant tmp = arg0.neg().add(this);
    //tmp now holds this - arg0
    return tmp.num.signum();
  }

  public FloatConstant multiply(FloatConstant other) {
    if (isZero()) {
      return ZERO;
    }
    if (other.isZero()) {
      return ZERO;
    }
    if (other.isOne()) {
      return this;
    }
    if (isOne()) {
      return other;
    }
    return FloatConstant.valueOf(num.multiply(other.num), den.multiply(other.den));
  }

  public FloatConstant add(FloatConstant other) {
    if (isZero()) {
      return other;
    }
    if (other.isZero()) {
      return this;
    }
    //a/b + c/d = ad + bc / bd
    BigInteger a = num;
    BigInteger b = den;
    BigInteger c = other.num;
    BigInteger d = other.den;

    BigInteger m = b.max(d);
    BigInteger n = b.min(d);

    return FloatConstant.valueOf((a.multiply(d).divide(n)).add(b.multiply(c).divide(n)), m);
  }
  public FloatConstant neg() {
    if (isZero()) {
      return this;
    }
    return FloatConstant.valueOf(num.negate(), den);
  }
  private static int numBits(int a) {
    int toRet = 1;
    long test = 2;
    while(a >= test) {
      toRet++;
      test <<= 1;

      if (toRet > 32) {
        throw new RuntimeException("Float constant numerator or denominator too large: "+a);
      }
    }
    return toRet;
  }
  public Type getType() {
    if (isOne() || isZero()) {
      return new BooleanType();
    }
    if (den.signum() <= 0){
      throw new RuntimeException("Assertion Error: Nonpositive denominator!");
    }
    BigInteger divisible = num.mod(den);
    if (divisible.signum() == 0) { //Divisibility check
      return IntConstant.valueOf(num.divide(den)).getType();
    }
    BigInteger numAbs = num.abs();
    BigInteger denAbs = den.abs();
    int na = numAbs.bitLength(); //number of bits needed to hold magnitude of numerator
    int nb = denAbs.subtract(BigInteger.ONE).bitLength(); //so 2 -> 1 -> 1, 3 -> 2 -> 2
    return new RestrictedFloatType(na, nb);
  }

  /**
   * Returns this (because a float is a primitive value)
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
    circuit.print(toString());
  }
  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    if (den.equals(BigInteger.ONE)) {
      return "C"+num;
    }
    return "C"+num+"/"+den;
  }

  /*
   * Public static constants
   */

  public static FloatConstant ONE = new FloatConstant(BigInteger.ONE, BigInteger.ONE);
  public static FloatConstant NEG_ONE = new FloatConstant(BigInteger.ONE.negate(),BigInteger.ONE);
  public static FloatConstant ZERO = new FloatConstant(BigInteger.ZERO, BigInteger.ONE);
}
