package SFE.Compiler.Operators;

import SFE.Compiler.BitString;
import SFE.Compiler.BlockStatement;
import SFE.Compiler.ConstExpression;
import SFE.Compiler.Expression;
import SFE.Compiler.Function;
import SFE.Compiler.IntConstant;
import SFE.Compiler.IntType;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.SLPTReduction;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;

public abstract class BitwiseOperator extends ScalarOperator implements SLPTReduction{
  private Type bitwiseEncoding;
  /**
   * (Mostly for bitwise operations), defines the bit length for arguments
   * (and results) of this operator.
   * 
   * I.E. A left shift performed by a ShiftOperator may truncate high
   * bits of the result based on its argument type. 
   */
  public void setBitwiseEncoding(Type type){
    this.bitwiseEncoding = type;
  }
  public Type getBitwiseEncoding(){
    return bitwiseEncoding;
  }
  /**
   * Should return the i'th output bit of this operator applied to the bitstring arguments.
   */
  public abstract Expression getOutputBit(int i, Expression ... args);

  /**
   * Subclasses can specify whether the i'th argument needs to be converted to a bit string.
   * By default, all arguments are converted to a bit string.
   */
  public boolean needsBitStringArgument(int i){
    return true;
  }
  /**
   * Most bitwise operators can fit into this template:
   * 1) Make a bitstring from all arguments
   * 2) Make a bitstring of the output bits, as a function of the bistrings from (1)
   * 3) Return a polynomial combining the bitstring of the output together, as specified by the bitwise encoding. 
   */
  public Expression inlineOp(StatementBuffer sb, Expression... args) {
    //Can we resolve without adding additional variables?
    {
      Expression got = resolve(args);
      if (got != null){
        return got;
      }
    }
    
    for(int i = 0; i < args.length; i++){
      if (needsBitStringArgument(i)){
        //Then args[i] must resolve to an lval or constant.
        LvalExpression le = LvalExpression.toLvalExpression(args[i]);
        IntConstant ie = IntConstant.toIntConstant(args[i]);
        if (ie == null && le == null){
          return null; //Can't do it.
        }
      }
    }
    
    Expression[] newArgs = new Expression[args.length];
    for(int i = 0; i < newArgs.length; i++){
      Expression got = args[i];
      if (!needsBitStringArgument(i)){
        newArgs[i] = got;
      } else {
        //We already showed above that the following call will not fail.
        newArgs[i] = BitString.toBitString(bitwiseEncoding, sb, args[i]);
      }
    }
    
    //Create a new bit string of the result variables
    int N = IntType.getBits((IntType)getBitwiseEncoding());
    Expression[] exprs = new Expression[N];
    BlockStatement block = new BlockStatement();
    for(int i = 0; i < N; i++){
      exprs[i] = getOutputBit(i, newArgs);

      //This is necessary - we can't have expressions inside bitstrings because a bitstring needs
      //to be able to decay to a polynomial at any time without creating variables 
      
      //Try to expand exprs[i] as a constant
      ConstExpression ce = ConstExpression.toConstExpression(exprs[i]);
      if (ce != null){
        exprs[i] = ce;
      } else {
        LvalExpression le = LvalExpression.toLvalExpression(exprs[i]);
        if (le != null){
          exprs[i] = le;
        } else {
          //Store it in a new lvalue
          exprs[i] = exprs[i].evaluateExpression("__bitwiseop:"+uid, ""+i, block);
        }
      }
    }
    uid++; //We don't have enough naming information to differentiate the bit strings, so do a cludgy uid hack. XXX
    block.toAssignmentStatements(sb);
    
    BitString result = new BitString(getBitwiseEncoding(), exprs);
    //Reroute result to use the new Lvals produced by the above.
    result = result.changeReference(Function.getVars());
    
    return result;
  }
  private static int uid = 0;
}
