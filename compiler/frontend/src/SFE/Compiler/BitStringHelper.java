package SFE.Compiler;

/**
 * Some useful conversions between C data types and bit strings, specifically when providing
 * APIS which can accept EITHER a pointer to a struct (which must be converted to bits) OR a bit string 
 * of bits representing said struct. 
 */
public class BitStringHelper {

  public static class BitStringStoreTarget {
    public BitString targetBits;
    public AssignmentStatement[] dummyAssignmentsToTargetBits;
    public LvalExpression targetCVar;
  }

  /**
   * Takes either:
   *  1) a bitString as input, in which case the bitString is simply returned
   *  2) a pointer as input, which is dereferenced, a new bitString is created
   *  of intermediate variables which are free to be assigned (length of bitString determined
   *  by the declared type of the pointed-to object.). 
   *  In this case, targetCVar is the pointed-to object. Any created intermediate variable
   *  come with a sub-assignment statement, whose outputLine is generated with a call to Program.getLineNumber()
   *  
   *  This method should be called in the phase when toAssignments() is being called. 
   *  (i.e. after toSLPTCircuit)
   */
  public static BitStringStoreTarget toBitStringStoreTarget(Expression ptrToDataOrBS) {
    BitStringStoreTarget toRet = new BitStringStoreTarget();
    
    if (ptrToDataOrBS instanceof BitString){
      toRet.targetBits = (BitString)ptrToDataOrBS;
      toRet.targetCVar = null;
    } else {
      Pointer ptr = Pointer.toPointerConstant(ptrToDataOrBS);
      LvalExpression target = toRet.targetCVar = ptr.access();
      Type targetType = target.getDeclaredType();
      int numBits = BitString.getBits(targetType); 

      LvalExpression[] bits = new LvalExpression[numBits];
      for(int i = 0; i < numBits; i++){
        bits[i] = Function.getVars().addVar(target.getName()+BitString.BIT_SEPARATOR_CHAR+i, new BooleanType(), false, false);
      }
      
      toRet.targetBits = new BitString(new RestrictedUnsignedIntType(numBits), bits);
    }
    
    //Create subStatements for each bit, this simply labels the created bits with their output lines
    AssignmentStatement[] subStatements = new AssignmentStatement[toRet.targetBits.size()];
    for(int i = 0; i < subStatements.length; i++){
      Expression bitStore = (Expression)toRet.targetBits.fieldEltAt(i);
      if (!(bitStore instanceof LvalExpression)){
        throw new RuntimeException("Assertion error: cannot store to non-lval: "+bitStore);
      }
      subStatements[i] = new AssignmentStatement((LvalExpression)bitStore, new Expression[0]);
      subStatements[i].setOutputLine(Program.getLineNumber());
    }
    toRet.dummyAssignmentsToTargetBits = subStatements;
    
    return toRet;
  }

  /**
   * Takes either:
   *  1) a bitString as input, in which case the bitString is simply returned
   *  2) a pointer as input, which is dereferenced, and the pointed-to bitString is returned
   *  
   *  This method should be called in the phase when toAssignments() is being called. 
   *  (i.e. after toSLPTCircuit)
   */
  public static BitString toBitStringFromPtr(Expression ptrToDataToPut, StatementBuffer assignments) {
    BitString bitsToRet;
    if (ptrToDataToPut instanceof BitString){
      bitsToRet = (BitString)ptrToDataToPut;
    } else {
      Pointer ptr = Pointer.toPointerConstant(ptrToDataToPut);
      LvalExpression actualData = ptr.access();
      //Turn into a bit string of that many bits.
      //Get all derived bits and split them up.
      Type t = actualData.getDeclaredType();
      bitsToRet = BitString.toBitString(t, assignments, actualData);
    };
    return bitsToRet;
  }
}
