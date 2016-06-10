package SFE.Compiler.Operators;

import SFE.Compiler.AnyType;
import SFE.Compiler.BinaryOpExpression;
import SFE.Compiler.Expression;
import SFE.Compiler.IntConstant;
import SFE.Compiler.Type;

public class BitwiseXOROperator extends BitwiseOperator {
  public String toString(){
    return "bitwise^";
  }
  public int arity() {
    return 2;
  }
  public int priority() {
    throw new RuntimeException("Not implemented");
  }
  public Type getType(Object obj) {
    //No information until we've resolved the pointer during inlining.
    return new AnyType();
  }
  /**
   * Defers most of the expansion until inlineOp.
  public BlockStatement toSLPTCircuit(Object obj) {
    AssignmentStatement as     = ((AssignmentStatement) obj);
    LvalExpression      lhs    = as.getLHS(); //LHS of the param statement
    BinaryOpExpression  rhs    = (BinaryOpExpression) (as.getRHS());
    BlockStatement      result = new BlockStatement();

    Expression          right = rhs.getRight();
    Expression          left  = rhs.getLeft();


    result.addStatement(new AssignmentStatement(
        lhs.lvalFieldEltAt(0),
        new BinaryOpExpression(this, left.fieldEltAt(0), right.fieldEltAt(0))
      ));
    
    return result;
  }
   */
  public Expression getOutputBit(int i, Expression ... args) {
    return new BinaryOpExpression(new XOROperator(), args[0].fieldEltAt(i), args[1].fieldEltAt(i));
  }
  public IntConstant resolve(Expression ... args) {
    //Without the power of creating additional variables, we must have constant arguments.
    /*
    Expression left = args[0];
    Expression right = args[1];

    IntConstant lc = IntConstant.toIntConstant(left);
    IntConstant rc = IntConstant.toIntConstant(right);
    if (lc != null && rc != null){
      return new IntConstant(lc.value() ^ rc.value());
    }
    */
    return null;
  }
}
