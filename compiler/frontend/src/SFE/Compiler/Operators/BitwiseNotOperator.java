package SFE.Compiler.Operators;

import SFE.Compiler.AnyType;
import SFE.Compiler.Expression;
import SFE.Compiler.IntConstant;
import SFE.Compiler.Type;
import SFE.Compiler.UnaryOpExpression;

public class BitwiseNotOperator extends BitwiseOperator{
  public BitwiseNotOperator(){
  }
  public String toString(){
    return "bitwisenot";
  }
  public int arity() {
    return 1;
  }
  public int priority() {
    throw new RuntimeException("Not implemented");
  }
  public Type getType(Object obj) {
    //No information until we've resolved the pointer during inlining.
    return new AnyType();
  }
  /* 
   * Left shifts are positive shifts, right shifts are negative.
   */
  public Expression getOutputBit(int i, Expression ... args) {
    return new UnaryOpExpression(new NotOperator(), args[0].fieldEltAt(i));
  }
  public IntConstant resolve(Expression ... args) {
    //Without the power of creating additional variables, we must have constant arguments.
    /* The following code is not safe, because it doesn't handle signed arguments correctly.
    Expression left = args[0];
    Expression right = args[1];
  
    IntConstant lc = IntConstant.toIntConstant(left);
    IntConstant rc = IntConstant.toIntConstant(right);
    if (lc != null && rc != null){
      if (direction == LEFT_SHIFT){
        return new IntConstant(lc.value() << rc.value());
      } else if (direction == RIGHT_SHIFT){
        return new IntConstant(lc.value() >>> rc.value());
      }
    }
    */
    return null;
  }

}
