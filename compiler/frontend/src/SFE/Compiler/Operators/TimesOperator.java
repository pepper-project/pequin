// TimesOperator.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler.Operators;

import java.io.PrintWriter;

import SFE.Compiler.BinaryOpExpression;
import SFE.Compiler.BooleanType;
import SFE.Compiler.Expression;
import SFE.Compiler.FiniteType;
import SFE.Compiler.FloatConstant;
import SFE.Compiler.FloatType;
import SFE.Compiler.IntConstant;
import SFE.Compiler.IntType;
import SFE.Compiler.OutputsToPolynomial;
import SFE.Compiler.PolynomialExpression;
import SFE.Compiler.PolynomialExpression.PolynomialTerm;
import SFE.Compiler.RestrictedFloatType;
import SFE.Compiler.RestrictedSignedIntType;
import SFE.Compiler.RestrictedUnsignedIntType;
import SFE.Compiler.SLPTReduction;
import SFE.Compiler.ScalarType;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;


/**
 * A class for representing binary * operator expressions that can be defined
 * in the program.
 */
public class TimesOperator extends ScalarOperator implements SLPTReduction, OutputsToPolynomial {
  //~ Methods ----------------------------------------------------------------

  /**
   * Returns a string representation of the object.
   */
  public String toString() {
    return "*";
  }

  /**
   * Returns 2 as the arity of this TimesOperator.
   * Arity is 1 for unary ops; 2 for binary ops; 3 for ternary ops;
   0 for constants
   * @return 2 as the arity of this TimesOperator.
   */
  public int arity() {
    return 2;
  }

  public Expression inlineOp(StatementBuffer assignments, Expression ... args) {
    if (args.length != 2) {
      throw new RuntimeException("TimesOperator is binary");
    }

    Expression right = args[0];
    Expression left = args[1];

    PolynomialExpression pleft = PolynomialExpression.toPolynomialExpression(left);
    PolynomialExpression pright = PolynomialExpression.toPolynomialExpression(right);
    if (pleft != null && pright != null) {
      if (pleft.getDegree() + pright.getDegree() <= 2) {
        PolynomialExpression product = new PolynomialExpression();
        PolynomialTerm productTerm = new PolynomialTerm();
        productTerm.addFactor(pleft);
        productTerm.addFactor(pright);
        product.addMultiplesOfTerm(IntConstant.ONE, productTerm);
        return product;
      }
    }

    return null;
  }
  
  public Expression resolve(Expression ... args) {
    Expression left = args[0];
    Expression right = args[1];

    FloatConstant lc = FloatConstant.toFloatConstant(left);
    FloatConstant rc = FloatConstant.toFloatConstant(right);
    if (lc != null && rc != null){
      return lc.multiply(rc);
    }
    if (lc != null && lc.isOne()){
      return right;
    }
    if (rc != null && rc.isOne()){
      return left;
    }
    return null;
  }

  /*
  public void updateRequiredBits(Object obj, Lvalue lvalue) {
  	BinaryOpExpression expr = (BinaryOpExpression) obj;

  	Expression left = expr.getLeft();
  	Expression right = expr.getRight();

  	Lvalue tempLeft = new VarLvalue(new Variable("operatorBits$Left", new FloatType()), false);
  	Lvalue tempRight = new VarLvalue(new Variable("operatorBits$Right", new FloatType()), false);

  	((PrimitiveTypeBounds)left).updateRequiredBits(null, tempLeft);
  	((PrimitiveTypeBounds)right).updateRequiredBits(null, tempRight);

  	if (tempLeft.isUnbounded() || tempRight.isUnbounded()){
  		return;
  	}

  	if (tempLeft.isBoolean()){
  		lvalue.updateRequiredBits(tempRight);
  		return;
  	}
  	if (tempRight.isBoolean()){
  		lvalue.updateRequiredBits(tempLeft);
  		return;
  	}
  	//Neither left, nor right, is boolean
  	if (tempLeft.isZeroOrPMOne()){
  		lvalue.updateRequiredBits(tempRight);
  		return;
  	}
  	if (tempRight.isZeroOrPMOne()){
  		lvalue.updateRequiredBits(tempLeft);
  		return;
  	}
  	//General case: add numerator and denominator bits.
  	lvalue.updateRequiredBits(tempLeft.getRequiredNumeratorBits() + tempRight.getRequiredNumeratorBits(),
  			tempLeft.getRequiredDenominatorBits() + tempRight.getRequiredDenominatorBits());
  }
  */

  public static void main(String[] args) {
    //Test type multiplication
    System.out.println(multiplyTypes(new RestrictedFloatType(1, 1), new RestrictedUnsignedIntType(1)));
    System.out.println(multiplyTypes(new RestrictedFloatType(1, 2), new RestrictedSignedIntType(2)));
    System.out.println(multiplyTypes(new RestrictedFloatType(1, 2), new BooleanType()));
    System.out.println(multiplyTypes(new RestrictedSignedIntType(32), new RestrictedSignedIntType(32)));
    System.out.println(RestrictedSignedIntType.getNeededLength(IntConstant.negExp2(31).multiply(IntConstant.negExp2(31))));
  }
  public Type getType(Object obj) {
    BinaryOpExpression expr = (BinaryOpExpression) obj;

    Expression left = expr.getLeft();
    Expression right = expr.getRight();

    return multiplyTypes(left.getType(), right.getType());
  }
  public static Type multiplyTypes(Type a, Type b) {
    if (!(a instanceof FiniteType && b instanceof FiniteType)) {
      if ((a instanceof ScalarType) && (b instanceof ScalarType)){
        if ((a instanceof FloatType) || (b instanceof FloatType)){
          return new FloatType(); //Infinite float product
        }
        return new IntType(); //Infinite int product
      }
      throw new ClassCastException("Type error: Multiplication not defined for infinite types: "+a+" * "+b);
    }
    if (a instanceof BooleanType) {
      return b;
    }
    if (b instanceof BooleanType) {
      return a;
    }

    if (a instanceof RestrictedUnsignedIntType) {
      if (b instanceof RestrictedUnsignedIntType) {
        return multiplyTypes_uu((RestrictedUnsignedIntType)a,(RestrictedUnsignedIntType)b);
      }
      if (b instanceof RestrictedSignedIntType) {
        return multiplyTypes_us((RestrictedUnsignedIntType)a,(RestrictedSignedIntType)b);
      }
      if (b instanceof RestrictedFloatType) {
        return multiplyTypes_uf((RestrictedUnsignedIntType)a,(RestrictedFloatType)b);
      }
    }
    if (a instanceof RestrictedSignedIntType) {
      if (b instanceof RestrictedUnsignedIntType) {
        return multiplyTypes_us((RestrictedUnsignedIntType)b,(RestrictedSignedIntType)a);
      }
      if (b instanceof RestrictedSignedIntType) {
        return multiplyTypes_ss((RestrictedSignedIntType)a,(RestrictedSignedIntType)b);
      }
      if (b instanceof RestrictedFloatType) {
        return multiplyTypes_sf((RestrictedSignedIntType)a,(RestrictedFloatType)b);
      }
    }
    if (a instanceof RestrictedFloatType) {
      if (b instanceof RestrictedUnsignedIntType) {
        return multiplyTypes_uf((RestrictedUnsignedIntType)b,(RestrictedFloatType)a);
      }
      if (b instanceof RestrictedSignedIntType) {
        return multiplyTypes_sf((RestrictedSignedIntType)b,(RestrictedFloatType)a);
      }
      if (b instanceof RestrictedFloatType) {
        return multiplyTypes_ff((RestrictedFloatType)a,(RestrictedFloatType)b);
      }
    }
    
    throw new ClassCastException("Type error: Cannot define multiplication of "+a+" and "+b);
  }

  private static Type multiplyTypes_ff(RestrictedFloatType a, RestrictedFloatType b) {
    int newNa = a.getNa() + b.getNa();
    if (a.getNa() == 1) {
      newNa = b.getNa();
    }
    if (b.getNa() == 1) {
      newNa = a.getNa();
    }
    return new RestrictedFloatType(newNa, a.getNb() + b.getNb());
  }

  private static Type multiplyTypes_sf(RestrictedSignedIntType a, RestrictedFloatType b) {
    //(2^{a-1} - 1)*(2^{Na} - 1) = 2^{a + Na - 1} - 2^{a - 1} - 2^{Na} + 1 <= 2^{a - 1 + Na} - 1
    //(-2^{a-1})*(2^{Na} - 1) = -2^{a + Na - 1} + 2^{a - 1} >= -2^{a - 1 + Na} + 1
    //denominator unaffected
    return new RestrictedFloatType(a.getLength() - 1 + b.getNa(), b.getNb());
  }

  private static Type multiplyTypes_ss(RestrictedSignedIntType a, RestrictedSignedIntType b) {
    //(-2^{a})*(-2^{b}) = 2^{a + b} <= 2^{a+b+1} - 1
    //(-2^{a})*(2^{b}-1) = -2^{a+b} + 2^{a} >= -2^{a+b}
    return new RestrictedSignedIntType(a.getInterval().multiply(b.getInterval()));
  }

  private static Type multiplyTypes_uf(RestrictedUnsignedIntType a, RestrictedFloatType b) {
    //(2^{a} - 1)*(2^{Na} - 1) = 2^{a + Na} - 2^{a} - 2^{Na} + 1 <= 2^{a + Na} - 1
    //denominator unaffected
    return new RestrictedFloatType(a.getLength() + b.getNa(), b.getNb());
  }

  private static Type multiplyTypes_us(RestrictedUnsignedIntType a, RestrictedSignedIntType b) {
    //(2^{a} - 1)*(-2^{b-1}) = -2^{a + b -1} + 2^{b-1} >= -2^{a+b-1}
    //(2^{a} - 1)*(2^{b-1} - 1) = 2^{a+b-1} - 2^{a} - 2^{b-1} + 1 <= 2^{a+b-1} - 1
    return new RestrictedSignedIntType(a.getInterval().multiply(b.getInterval()));
  }

  private static Type multiplyTypes_uu(RestrictedUnsignedIntType a, RestrictedUnsignedIntType b) {
    //return new RestrictedUnsignedIntType(a.getLength() + b.getLength());
    return new RestrictedUnsignedIntType(a.getInterval().multiply(b.getInterval()));
  }

  /**
   * Returns an int that represents the priority of the operator
   * @return an int that represents the priority of the operator
   */
  public int priority() {
    return 3;
  }

  public void toCircuit(PrintWriter circuit) {
    circuit.print("gate poly");
  }

  public static int maxTermsWhenInliningSquare = 3;
}
