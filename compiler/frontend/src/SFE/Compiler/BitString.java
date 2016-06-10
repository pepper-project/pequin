package SFE.Compiler;

import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Vector;

import SFE.Compiler.Operators.ArrayAccessOperator;
import SFE.Compiler.Operators.StructAccessOperator;

public class BitString extends Expression implements OutputWriter{
  public static char BIT_SEPARATOR_CHAR = '!';
  
  private Expression[] bits;
  private Type bitwiseEncoding;
  public BitString(Type bitwiseEncoding, Expression[] bits){
    this.bits = bits;
    /*
    for(Expression q : bits){
      if (q instanceof OperationExpression){
        throw new RuntimeException("Cannot have operation expressions inside a bitString.");
      }
    }
    */
    this.bitwiseEncoding= bitwiseEncoding;
  }
  public Type getBitwiseEncoding() {
    return bitwiseEncoding;
  }
  public Expression fieldEltAt(int i){
    /*
    if (i >= bits.length){
      return IntConstant.ZERO; //This allows easy conversion between longs, ints, etc.
    }
    */
    return bits[i];
  }
  public int size() {
    return bits.length;
  }
  public BitString changeReference(VariableLUT unique) {
    for(int i = 0; i < bits.length; i++){
      bits[i] = bits[i].changeReference(unique);
    }
    return this;
  }
  public Collection<LvalExpression> getLvalExpressionInputs() {
    throw new RuntimeException("Not implemented");
    //return new ArrayList(); //Not helpful, but the polynomial extracted from this bitstring should be handling this.
  }
  public Type getType() {
    return new AnyType(); //Again, not helpful, but the polynomial extracted from this bitstring does the heavy lifting.
  }
  /**
   * Converts expression c to a bitstring of format bitwiseEncoding. Some statements may be created
   * as a result of the conversion. 
   * 
   * If forceTranslation is false, then the compiler will attempt to use type information of c to 
   * dynamically size the bitstring. If this is not possible, a bitstring of the format specified in bitwiseEncoding
   * is returned.
   */
  public static BitString toBitString(Type bitwiseEncoding, StatementBuffer sb, Expression c) {
    c = c.fullyResolve(c);
    
    BitString[] subStrings = new BitString[bitwiseEncoding.size()];
    for(int i = 0; i < subStrings.length; i++){
      subStrings[i] = toBitString_((IntType)bitwiseEncoding.fieldEltTypeAt(i), sb, c.fieldEltAt(i));
    }
    return concatenate(subStrings);
  }
  private static BitString toBitString_(IntType bitwiseEncoding, StatementBuffer sb, Expression c) {
    /*
    if (bitwiseEncoding.hasDerives()){
      //Recursively encode more complicated types.
      if (bitwiseEncoding instanceof ArrayType){
        ArrayType at = (ArrayType)bitwiseEncoding;
        int len = at.getLength();
        BitString[] subStrings = new BitString[len];
        for(int i = 0; i < len; i++){
          LvalExpression subVal = new ArrayAccessOperator().resolve(c, IntConstant.valueOf(i));
          subStrings[i] = toBitString(subVal.getDeclaredType(), sb, subVal);
        }
        return concatenate(subStrings);
      } else if (bitwiseEncoding instanceof StructType){
        StructType st = (StructType)bitwiseEncoding;
        Vector<String> fields = st.getFields();
        BitString[] subStrings = new BitString[fields.size()];
        int p = 0;
        for(String q : fields){
          LvalExpression subVal = new StructAccessOperator(q).resolve(c);
          subStrings[p++] = toBitString(subVal.getDeclaredType(), sb, subVal);
        }
        return concatenate(subStrings);
      } else {
        throw new RuntimeException("BitString doesn't know how to expand type "+bitwiseEncoding+" to bits.");
      }
    }
    */
    int N = IntType.getBits((IntType)bitwiseEncoding);
    
    Expression[] bs = new Expression[N];
    boolean signed = bitwiseEncoding instanceof RestrictedSignedIntType;
    IntConstant cv = IntConstant.toIntConstant(c);
    if (cv != null){
      BigInteger val = cv.value();
      
      /*
      Losing precision from constants happens sometimes. We can't assert it never happens.
      int requiredBits = val.bitLength() + (signed ? 1 : 0);
      if (requiredBits > N){
        throw new RuntimeException("ERROR: Loss of precision representing integer "+cv+" as bitstring of type "+bitwiseEncoding);
      }
      */
      
      for(int i = 0; i < N; i++, val = val.shiftRight(1)){
        if (i == N-1 && signed){
          bs[i] = BooleanConstant.valueOf(cv.signum() < 0);
        } else {
          bs[i] = BooleanConstant.valueOf(val.testBit(0));
        }
      }
      return new BitString(bitwiseEncoding, bs);
    }

    //TODO: sync this file with zaatar git.
    
    LvalExpression asLval = LvalExpression.toLvalExpression(c);
    if (asLval != null){
      //Look for a bitstring in the lval itself
      BitString oldString = asLval.getBitString();
      if (oldString != null){
        return translate(oldString, bitwiseEncoding);
      }
    
      //Make a fresh bit string.
      
      //When lazily creating a bit string, we need to expand the bits into the same scope level
      //as the original variable.
      
      Map varScope = Function.getVars().getVarScope(asLval.getName());
      
      //Expand the Lval to its bit string (dependent on its current type)
      int bitsInLval = IntType.getBits((IntType)asLval.getType()); //Ensure that we have enough space to hold the split bits
      LvalExpression[] split = new LvalExpression[bitsInLval];
      for(int i = 0; i < bitsInLval; i++){
        String name = asLval.getName()+BIT_SEPARATOR_CHAR+i;
        split[i] = new LvalExpression(new VarLvalue(new Variable(name, new BooleanType()), false));
        varScope.put(name, split[i]);
      }
      SplitStatement s = new SplitStatement(asLval.getType(), asLval, split);
      s.toAssignmentStatements(sb);
      BitString newBs = new BitString(asLval.getType(), split);
      
      //Tell the lval about its bitString
      asLval.setBitString(newBs);
      
      //Translate this to the bitstring format we want.
      return translate(newBs, bitwiseEncoding);
    } else {
      throw new RuntimeException("I don't know how to make a Bitstring from "+c);
    }
  }

  /**'
   * If substrings has length one, returns the first substring.
   * 
   * Otherwise, 
   * Concatenates bit strings, returning a bit string with encoding RestrictedUnsignedIntType
   * of all the substrings concatenated (index 0 in the returned string refers to a bit in the first argument.
   */
  public static BitString concatenate(BitString ... subStrings) {
    if (subStrings.length == 1){
      return subStrings[0]; 
    }
    
    int numBits = 0;
    for(BitString bs : subStrings){
      numBits += bs.size();
    }
    Expression[] bits = new Expression[numBits];
    int p = 0;
    for(BitString bs : subStrings){
      for(int i = 0; i < bs.size(); i++){
        bits[p+i] = bs.fieldEltAt(i);
      }
      p += bs.size();
    }
    return new BitString(new RestrictedUnsignedIntType(numBits), bits);
  }
  private static BitString translate(BitString bs, Type bitwiseEncoding) {
    int lengthOld = IntType.getBits((IntType)bs.bitwiseEncoding);
    int lengthNew = IntType.getBits((IntType)bitwiseEncoding);
    boolean signedOld = bs.bitwiseEncoding instanceof RestrictedSignedIntType;
    boolean signedNew = bitwiseEncoding instanceof RestrictedSignedIntType;
    
    /*
    if (!signedOld && !signedNew && lengthNew < lengthOld){
      //Refuse to do unsigned-to-unsigned truncations, to maintain compatibility with gmp 
      gmp_test broken, need to find a workaround.
      return bs;
    }
    */
    
    boolean extendSignBit = signedOld;
    boolean extendedBits = false;
    
    Expression[] exprs = new Expression[lengthNew];
    Arrays.fill(exprs, BooleanConstant.FALSE);
    for(int i = 0; i < lengthNew; i++){
      if (i < lengthOld){
        exprs[i] = bs.fieldEltAt(i);
      } else {
        if (extendSignBit){ //Extend the sign bit to the remaining bits
          exprs[i] = bs.fieldEltAt(lengthOld-1);
          extendedBits = true;
        } else {
          //Fill with zeros.
        }
      }
    }

    /*
    if (extendedBits){
      System.out.println("!");
    }
    */
    
    return new BitString(bitwiseEncoding, exprs);
  }
  /**
   * Interprets bits (in two's complement form, if signed) to a polynomial representation.
  public PolynomialExpression bitsAsInteger() {
    int N = IntType.getBits((IntType)bitwiseEncoding);
    boolean signed = bitwiseEncoding instanceof RestrictedSignedIntType;
    
    int pot = 1;
    if (N > 32){
      throw new RuntimeException("Converting bits to long int not yet supported.");
    }
    final Type targetType;
    if (signed){
      targetType = new RestrictedSignedIntType(N);
    } else {
      targetType = new RestrictedUnsignedIntType(N);
    }
    PolynomialExpression pe = new PolynomialExpression(){
      public Type getType(){
        return targetType;
      }
    };
    for(int i = 0; i < N; i++, pot <<= 1){
      int signedPot = pot;
      if (i == N-1 && signed){
        signedPot = -signedPot;
      }
      PolynomialTerm pt = new PolynomialTerm();
      pt.addFactor(bits[i]);
      pe.addMultiplesOfTerm(IntConstant.valueOf(signedPot), pt);
    }
    return pe;
  }
   */
  public void addReference() {
    for(Expression q : bits){
      if (q instanceof LvalExpression){
        ((LvalExpression) q).addReference();
      }
    }
  }
  public void toCircuit(Object obj, PrintWriter circuit) {
    for(Expression q : bits){
      ((OutputWriter)q).toCircuit(null, circuit);
      circuit.print(" ");
    }
  }
  /**
   * Outputs assignment statements assigning to the appropriate elements of target the bits of this string.
   * 
   * Takes care of things like assigning a string of 64 bits to a struct store {uint32_t a, int32_t b}. 
   * Uses getDeclaredType to determine the type of lvals.
   */
  public void toAssignments(LvalExpression target, StatementBuffer assignments) {
    Type targetType = target.getDeclaredType();
    
    if (getBits(targetType) != size()){
      throw new RuntimeException("Type error: A bit string of length "+size()+" is incompatible with type "+targetType+", requiring "+getBits(targetType)+" bits.");
    }
    
    /*
    if (targetType.hasDerives()){
      //Recursively encode more complicated types.
      if (targetType instanceof ArrayType){
        ArrayType at = (ArrayType)targetType;
        int len = at.getLength();
        int pos = 0;
        for(int i = 0; i < len; i++){
          LvalExpression subVal = new ArrayAccessOperator().resolve(target, IntConstant.valueOf(i));
          int subLength = getBits(subVal.getDeclaredType());
          BitString subString = substring(pos, pos + subLength);
          subString.toAssignments(subVal, assignments);
          pos += subLength;
        }
      } else if (targetType instanceof StructType){
        StructType st = (StructType)targetType;
        Vector<String> fields = st.getFields();
        int pos = 0;
        for(String q : fields){
          LvalExpression subVal = new StructAccessOperator(q).resolve(target);
          int subLength = getBits(subVal.getDeclaredType());
          BitString subString = substring(pos, pos + subLength);
          subString.toAssignments(subVal, assignments);
          pos += subLength;
        }
      } else {
        throw new RuntimeException("BitString doesn't know how to expand type "+bitwiseEncoding+" to bits.");
      }
      */
    
    
    int pos = 0;
    for(int i = 0; i < targetType.size(); i++){
      LvalExpression subVal = target.fieldEltAt(i);
      int subLength = getBits(subVal.getDeclaredType());
      BitString subString = substring(pos, pos + subLength);
      subString.toAssignments_(subVal, assignments);
      pos += subLength;
    }
  }
  //Scalar version of toAssignments
  private void toAssignments_(LvalExpression target, StatementBuffer assignments) {
    Type targetType = target.getDeclaredType();
    
    if (getBits(targetType) != size()){
      throw new RuntimeException("Type error: A bit string of length "+size()+" is incompatible with type "+targetType+", requiring "+getBits(targetType)+" bits.");
    }

    if (bits.length != IntType.getBits((IntType)targetType)){
      throw new RuntimeException("Cannot assign a bit string of length "+bits.length+" to a variable of type "+target);
    }
    BitString newString = new BitString(targetType, bits); //Take signedness from the target type. 
    //PolynomialExpression poly = PolynomialExpression.toPolynomialExpression(newString);
    new AssignmentStatement(target, newString).toAssignmentStatements(assignments);
  }
  
  /**
   * Returns a bitstring of bits starting at start (inclusive) to index end (noninclusive).
   */
  public BitString substring(int start, int end) {
    int N = end - start;
    Expression[] bits = new Expression[N];
    for(int i = start; i < end; i++){
      bits[i-start] = fieldEltAt(i);
    }
    return new BitString(new RestrictedUnsignedIntType(N), bits);
  }
  /**
   * Outputs the number of bits needed to represent type t.
   * 
   * For int types, equivalent to IntType.getBits(t);
   */
  public static int getBits(Type t){
    //Recursively encode more complicated types.
    /*
    if (t instanceof ArrayType){
      ArrayType at = (ArrayType)t;
      int len = at.getLength();
      return len * getBits(at.getComponentType());
    } else if (t instanceof StructType){
      int toRet = 0;
      StructType st = (StructType)t;
      for(Type q : st.getFieldTypes()){
        toRet += getBits(q);
      }
      return toRet;
    }
    */
    int sum = 0;
    for(int i = 0; i < t.size(); i++){
      sum += getBits_(t.fieldEltTypeAt(i));
    }
    return sum;
  }
  //Scalar version of getBits
  private static int getBits_(Type t){
    return IntType.getBits((IntType)t);
  }
}
