package SFE.Compiler;

import SFE.Compiler.Operators.MinusOperator;
import SFE.Compiler.Operators.PlusOperator;
import SFE.Compiler.Operators.TimesOperator;
import SFE.Compiler.PolynomialExpression.PolynomialTerm;

public class BooleanExpressions {
  public static OperationExpression or(Expression a, Expression b) {
    //Verify that a and b are booleans
    checkBoolean(a);
    checkBoolean(b);

    //return (a-1)*(1-b) + 1
    return new BinaryOpExpression(new PlusOperator(), IntConstant.ONE,
                                  new BinaryOpExpression(new TimesOperator(),
                                      new BinaryOpExpression(new PlusOperator(), a, IntConstant.NEG_ONE),
                                      new BinaryOpExpression(new MinusOperator(), IntConstant.ONE, b)
                                                        )
                                 );
  }
  public static OperationExpression and(Expression a, Expression b) {
    //Verify that a and b are booleans
    checkBoolean(a);
    checkBoolean(b);

    //return ab
    return new BinaryOpExpression(new TimesOperator(), a, b);
  }
  public static OperationExpression not(Expression a) {
    //Verify that a is boolean
    checkBoolean(a);

    //return 1 - a

    //This doesn't preserve the type data.
    //return new BinaryOpExpression(new MinusOperator(), IntConstant.ONE, a);

    PolynomialExpression toRet = new PolynomialExpression();
    PolynomialTerm term = new PolynomialTerm();
    toRet.addMultiplesOfTerm(IntConstant.ONE, term);
    term = new PolynomialTerm();
    term.addFactor(a);
    toRet.addMultiplesOfTerm(IntConstant.NEG_ONE, term);

    return toRet;
  }
  public static OperationExpression xor(Expression a, Expression b) {
    //Verify that a and b are booleans
    checkBoolean(a);
    checkBoolean(b);

    /*
    if (a instanceof ConstExpression){
    	FloatConstant fc = FloatConstant.toFloatConstant((ConstExpression)a);
    	if (fc.isOne()){
    		return not(b);
    	} else {
    		return new UnaryOpExpression(new UnaryPlusOperator(), b);
    	}
    }

    if (b instanceof ConstExpression){
    	FloatConstant fc = FloatConstant.toFloatConstant((ConstExpression)b);
    	if (fc.isOne()){
    		return not(a);
    	} else {
    		return new UnaryOpExpression(new UnaryPlusOperator(), a);
    	}
    }
    */

    //return a + b - 2ab
    return new BinaryOpExpression(new PlusOperator(), a,
                                  new BinaryOpExpression(new PlusOperator(), b,
                                      new BinaryOpExpression(new TimesOperator(), IntConstant.valueOf(-2),
                                          new BinaryOpExpression(new TimesOperator(), a, b)
                                                            )
                                                        )
                                 );
  }

  private static void checkBoolean(Expression a) {
    if (!TypeHeirarchy.isSubType(a.getType(),new BooleanType())) {
      throw new IllegalArgumentException("Type error: Boolean operation on input: "+a+", which is not boolean.");
    }
  }
}
