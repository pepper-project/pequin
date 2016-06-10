package SFE.Compiler;

import SFE.Compiler.Operators.MinusOperator;
import SFE.Compiler.Operators.PlusOperator;
import SFE.Compiler.Operators.TimesOperator;


/**
 * The mux M ? A : B can be turned into the convex combination
 * 		   M * (A - B) + B
 */
public class MuxPolynomialExpression extends PolynomialExpression {

  /*
   * Fields
   */
  private LvalExpression condition;
  private LvalExpression valueTrue;
  private LvalExpression valueFalse;

  /**
   * Returns this mux as a nesting of binary operations:
   * M*(A - B) + B
   */
  public OperationExpression toBinaryOps() {
    BinaryOpExpression AMinB = new BinaryOpExpression(new MinusOperator(), valueTrue, valueFalse);
    BinaryOpExpression MAminB = new BinaryOpExpression(new TimesOperator(), condition, AMinB);
    return new BinaryOpExpression(new PlusOperator(), MAminB, valueFalse);
  }

  /*
   * Constructors
   */
  public MuxPolynomialExpression(LvalExpression condition, LvalExpression valueTrue, LvalExpression valueFalse) {
    this.condition = condition;
    this.valueTrue = valueTrue;
    this.valueFalse = valueFalse;

    //Construct the polynomial c*(valueTrue - valueFalse) + valueFalse
    {
      PolynomialTerm t = new PolynomialTerm();
      t.addFactor(new PolynomialMonomer(condition));
      {
        PolynomialExpression aMinB = new PolynomialExpression();
        PolynomialTerm valueTrue_ = new PolynomialTerm();
        valueTrue_.addFactor(valueTrue);
        PolynomialTerm valueFalse_ = new PolynomialTerm();
        valueFalse_.addFactor(valueFalse);
        aMinB.addMultiplesOfTerm(IntConstant.ONE, valueTrue_);
        aMinB.addMultiplesOfTerm(IntConstant.NEG_ONE, valueFalse_);

        t.addFactor(aMinB);
      }
      addMultiplesOfTerm(IntConstant.ONE, t);
    }

    {
      PolynomialTerm t = new PolynomialTerm();
      t.addFactor(new PolynomialMonomer(valueFalse));
      addMultiplesOfTerm(IntConstant.ONE, t);
    }
  }

  /*
   * Methods
   */
  public LvalExpression getCondition() {
    return condition;
  }

  public LvalExpression getValueTrue() {
    return valueTrue;
  }

  public LvalExpression getValueFalse() {
    return valueFalse;
  }

  public static MuxPolynomialExpression toMuxExpression(Expression p) {
    if (p instanceof MuxPolynomialExpression) {
      return (MuxPolynomialExpression)p;
    }
    if (p instanceof LvalExpression) {
      Statement as = ((LvalExpression)p).getAssigningStatement();
      if (as instanceof AssignmentStatement) {
        for(Expression q : ((AssignmentStatement)as).getAllRHS()) {
          MuxPolynomialExpression got = toMuxExpression(q);
          if (got != null) {
            return got;
          }
        }
      }
    }
    return null;
  }

  public Type getType() {
    return TypeHeirarchy.looseUnion(valueTrue.getType(), valueFalse.getType());
  }

}
