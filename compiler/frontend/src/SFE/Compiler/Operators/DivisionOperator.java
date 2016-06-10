package SFE.Compiler.Operators;

import java.io.PrintWriter;
import java.math.BigInteger;

import SFE.Compiler.BinaryOpExpression;
import SFE.Compiler.BlockStatement;
import SFE.Compiler.Expression;
import SFE.Compiler.FloatConstant;
import SFE.Compiler.Function;
import SFE.Compiler.IntConstant;
import SFE.Compiler.IntType;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.Optimizer;
import SFE.Compiler.OutputWriter;
import SFE.Compiler.RestrictedIntType;
import SFE.Compiler.RestrictedSignedIntType;
import SFE.Compiler.RestrictedUnsignedIntType;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;


public class DivisionOperator extends ScalarOperator implements OutputWriter{
  public static final int REMAINDER = 0,
                    QUOTIENT = REMAINDER+1;
  
  private int mode;
  public DivisionOperator(int mode){
    this.mode = mode;
  }
  
  public String toString(){
    switch(mode){
    case REMAINDER: return "%";
    case QUOTIENT: return "/";
    default: throw new RuntimeException("Assertion error");
    }
  }
  
  public int getMode(){
    return mode;
  }
  
  public int arity() {
    return 2;
  }
  public int priority() {
    throw new RuntimeException("Not implemented");
  }
  public Type getType(Object obj) {
    BinaryOpExpression boe = (BinaryOpExpression)obj;
    
    RestrictedIntType typeDividend = (RestrictedIntType) boe.getLeft().getType();
    RestrictedIntType typeDivisor = (RestrictedIntType) boe.getRight().getType();

    if (mode == REMAINDER){
      boolean isSignedRemainder = typeDividend instanceof RestrictedSignedIntType;
      
      return addOrRemoveSign(typeDivisor, isSignedRemainder);
    } else {
      boolean isSignedQuotient = (typeDividend instanceof RestrictedSignedIntType) 
            || (typeDivisor instanceof RestrictedSignedIntType);
      return addOrRemoveSign(typeDividend, isSignedQuotient);
    }
  }
  
  private IntType addOrRemoveSign(RestrictedIntType t, boolean shouldSign) {
    if (t instanceof RestrictedSignedIntType){
      if (shouldSign){
        return t;
      } else {
        //Restrict. This occurs when the dividend is unsigned and we are performing 
        //modular division with a signed divisor.
        return new RestrictedUnsignedIntType(IntType.getBits(t) - 1);
      }
    } else {
      if (shouldSign){
        //Expand. Occurs when dividend is unsigned and we are performing division
        //with a signed divisor.
        return new RestrictedSignedIntType(IntType.getBits(t) + 1);
      } else {
        return t;
      }
    }
  }

  private static class MagicNumber {
    public MagicNumber(BigInteger m, int shift) {
      this.M = m;
      this.shift = shift;
    }
    public BigInteger M;
    public int shift;
    public String toString(){
      return "multiply by "+M+" and then use p ="+shift;
    }
  }

  private MagicNumber getMagicNumber(IntType n_t, BigInteger d) {
    if (d.compareTo(BigInteger.valueOf(2)) < 0){
      throw new RuntimeException("I don't know how to compute the magic number for divisor "+d);
    }
    
    BigInteger nMax = IntType.getMaxInt(n_t);
    BigInteger nMin = IntType.getMinInt(n_t);
    
    for(int p = 0; true; p++){
      BigInteger twoP = BigInteger.ONE.shiftLeft(p);
      //Round twoP up to the next multiple of d, Md, then Md / d is an even integer.
      BigInteger Md = twoP.add(d).subtract(twoP.mod(d));
      //Furthermore, (Md - twoP)*n/(twoP) must be in [0, 1) for n nonnegative.
      if (Md.subtract(twoP).multiply(nMax).compareTo(twoP) >= 0){
        continue;
      }
      //Also, ((Md - twoP)*n + 1)/(twoP) must be in (-1, 0] for n negative.
      if (Md.subtract(twoP).multiply(nMin).add(BigInteger.ONE).negate().compareTo(twoP) >= 0){
        continue;
      }

      //Let M = Md / d.
      BigInteger M = Md.divide(d);
      //Let n >= 0.
      //Then Mn/twoP = Mdn/(dtwoP) = (Md - twoP)*n/(dtwoP) + n/d must be in n/d + [0, 1/d).
      //Hence, Mn >> p = n/d for n >= 0.
      
      //Let n < 0.
      //Then (Mn + twoP) >> p = Mn >> p + 1 = ceil(Mn/twoP + 1/(dtwoP))
      //and Mn/twoP + 1/(dtwoP) = 
      //(Mdn + 1)/(dtwoP) = ((Md - twoP)*n + twoP*n + 1)/(dtwoP)
      //= ceil((-1/d, 0] + n/d) = n/d. Done!
      
      //Note: Can use either (Mn + twoP) >> p or Mn >> p when n = 0.
      //But, when n = 1, ceil((-1/d, 0] + 1/d) = 1, whereas floor(1/d + [0,1/d)) = 0.
      //So we have a split.

      return new MagicNumber(M, p);
    }
  }
  
  public static void main(String[] args){
    BigInteger d = BigInteger.valueOf(7);
     
    DivisionOperator divisionOperator = new DivisionOperator(QUOTIENT);
    IntType testType = new RestrictedSignedIntType(6);
    MagicNumber mc = divisionOperator.getMagicNumber(testType, d);
    BigInteger twoP = BigInteger.ONE.shiftLeft(mc.shift);
    System.out.println(mc);
     
    //Division circuit:
    BigInteger maxInt = IntType.getMaxInt(testType);
    BigInteger minInt = IntType.getMinInt(testType);
    for (BigInteger n = minInt; n.compareTo(maxInt) <= 0; n = n.add(BigInteger.ONE)) {
       BigInteger q;
       if (n.signum()<0){
         
         //Floored division
         //-1 - (M(-n-1)) >> p)
         //q = BigInteger.valueOf(-1).subtract(mc.M.multiply(n.negate().subtract(BigInteger.ONE)).shiftRight(mc.shift));
         
         //Division
         //(Mn + twoP)>>p
         q = mc.M.multiply(n).add(twoP).shiftRight(mc.shift);
       } else {
         //Mn >> p
         q = mc.M.multiply(n).shiftRight(mc.shift);
       }
       System.out.println(n+" / "+d+" = "+q);       
     }
  }
  
  /*
  public BlockStatement toSLPTCircuit(Object obj) {
    AssignmentStatement as     = ((AssignmentStatement) obj);
    LvalExpression      lhs    = as.getLHS(); //LHS of the param statement
    BinaryOpExpression  rhs    = (BinaryOpExpression) (as.getRHS());
    BlockStatement      result = new BlockStatement();

    Expression          left  = rhs.getLeft();
    Expression          right = rhs.getRight();

    result.addStatement(new AssignmentStatement(
                          lhs.lvalFieldEltAt(0),
                          new BinaryOpExpression(this, left.fieldEltAt(0), right.fieldEltAt(0))
                        ));
    return result;
  }
  */
  
  public Expression inlineOp(StatementBuffer sb, Expression... args) {
    final IntConstant divisor = IntConstant.toIntConstant(args[1]);
    if (divisor == null){
      Expression safeArg0 = LvalExpression.toLvalExpression(args[0]);
      Expression safeArg1 = LvalExpression.toLvalExpression(args[1]);
      if (safeArg0 == null){
        //Other possibility is constants
        safeArg0 = IntConstant.toIntConstant(args[0]);
      }
      if (safeArg1 == null){
        //Other possibility is constants
        safeArg1 = IntConstant.toIntConstant(args[1]);
      }
      if (safeArg0 == null || safeArg1 == null){
        return null;
      }
      return new BinaryOpExpression(this, safeArg0, safeArg1);
      //throw new RuntimeException("Division by non-constant "+args[1]+" not supported.");
    }
    if (mode == REMAINDER && divisor.signum() < 0){
      throw new RuntimeException("Evaluating remainder by negative divisor not currently supported.");
    }
    if (divisor.equals(IntConstant.ZERO)){
      throw new RuntimeException("Division by zero.");
    }

    Expression arg0 = LvalExpression.toLvalExpression(args[0]);
    if (arg0 == null){
      arg0 = IntConstant.toIntConstant(args[0]);
    }
    if (arg0 == null){
      throw new RuntimeException("Assertion error");
    }
    
    if (divisor.equals(IntConstant.ONE)){
      switch(mode){
      case REMAINDER:
        return IntConstant.ZERO;
      case QUOTIENT:
        return args[0];
      default:
         throw new RuntimeException("Assertion error");
      }
    }
    
    
    //The approach below generates a magic number and performs a division with
    //constant divisor - but it creates variables, so we can't do it unless we
    //are on the first pass.
    
    if (!Optimizer.isFirstPass()){
      return null;
    }
    
    MagicNumber magicNumber = getMagicNumber((IntType)arg0.getType(), divisor.value());
    IntConstant twoP = IntConstant.valueOf(BigInteger.ONE.shiftLeft(magicNumber.shift));
    
    //Can we resolve without adding additional variables?
    IntConstant M = IntConstant.valueOf(magicNumber.M);
    
    BinaryOpExpression product = new BinaryOpExpression(new TimesOperator(), arg0, M);
    //if (arg0.getType() instanceof RestrictedSignedIntType){
      //if arg0 < 0, add twoP to product. Of course this isn't necessary if arg0 is unsigned.
      product = new BinaryOpExpression(new PlusOperator(), product,
          new BinaryOpExpression(new TimesOperator(),
              new BinaryOpExpression(new LessOperator(), arg0, IntConstant.ZERO), 
              twoP));
    //}
    
    BitwiseShiftOperator shiftRight = new BitwiseShiftOperator(BitwiseShiftOperator.RIGHT_SHIFT);
    shiftRight.setBitwiseEncoding(product.getType()); //HMMM investigate further.
    
    Expression quotient = new BinaryOpExpression(shiftRight, product, IntConstant.valueOf(magicNumber.shift));
    
    Expression result;
    switch(mode){
    case REMAINDER:
      //r = a - intdiv(a/d)*d = a - q*d;
      result = new BinaryOpExpression(new MinusOperator(), arg0, 
          new BinaryOpExpression(new TimesOperator(), quotient, divisor));
      break;
    case QUOTIENT:
      result = quotient;
      break;
    default:
      throw new RuntimeException("Assertion error");
    }
    
    BlockStatement block = new BlockStatement();
    Expression toRet = result.evaluateExpression("__divisionop:"+uid, "a", block);
    uid++; //We don't have enough naming information to differentiate the divisions, so do a cludgy uid hack. XXX
    block.toAssignmentStatements(sb);
    
    toRet = toRet.changeReference(Function.getVars());
    if (toRet instanceof LvalExpression){
      LvalExpression retLval = (LvalExpression)toRet;

      //Give the type system some help on the remainder case
      if (mode == REMAINDER){
        boolean isSignedRemainder = arg0.getType() instanceof RestrictedSignedIntType;
        retLval.getLvalue().setType(addOrRemoveSign((RestrictedIntType)divisor.getType(), isSignedRemainder));
      }
    }
    
    return toRet;
  }
  private static int uid = 0;

  public IntConstant resolve(Expression ... args) {
    Expression left = args[0];
    Expression right = args[1];

    FloatConstant lf = FloatConstant.toFloatConstant(left);
    if (lf != null){
      if (lf.isZero()){
        return IntConstant.ZERO;
      }
    }
    
    IntConstant lc = IntConstant.toIntConstant(left);
    IntConstant rc = IntConstant.toIntConstant(right);
    if (lc != null && rc != null){
      if (mode == QUOTIENT){
        return IntConstant.valueOf(lc.value().divide(rc.value()));
      } else {
        return IntConstant.valueOf(lc.value().remainder(rc.value()));
      }
    }
    return null;
  }

  public Expression getOutputBit(int i, Expression... args) {
    throw new RuntimeException("Not implemented");
  }

  public void toCircuit(Object obj, PrintWriter circuit) {
    BinaryOpExpression boe = (BinaryOpExpression)obj;
    ((OutputWriter)boe.getLeft()).toCircuit(null, circuit);
    circuit.print(" "+toString()+" ");
    ((OutputWriter)boe.getRight()).toCircuit(null, circuit);
  }
}
