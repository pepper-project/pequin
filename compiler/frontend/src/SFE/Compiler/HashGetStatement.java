package SFE.Compiler;

import java.io.PrintWriter;

import ccomp.CBuiltinFunctions;

/**
 * A statement which turns in a hash and receives the object the database has for that hash
 */
public class HashGetStatement extends StatementWithOutputLine implements OutputWriter{
  private Expression ptrToRecvData;
  private Expression ptrToHash;
  private BitString bitsHash; //Filled in during toAssignmentStatements  
  private BitString bitsToRecvDat; //Filled in during toAssignmentStatements
  private AssignmentStatement[] subStatements; //Filled in during toAssignmentStatements
  
  /**
   * Standard - expressions can be pointers to data, or a bitstring.
   */
  public HashGetStatement(Expression pointerToRecvData, Expression pointerToHash) {
    this.ptrToRecvData = pointerToRecvData;
    this.ptrToHash = pointerToHash;
  }

  public Statement toSLPTCircuit(Object obj) {
    if (!(ptrToRecvData instanceof BitString)){
      ptrToRecvData = ptrToRecvData; //.bitAt(0);
    }
    if (!(ptrToHash instanceof BitString)){
      ptrToHash = ptrToHash;//.bitAt(0);
    }
    return this; 
  }

  public Statement duplicate() {
    return new HashGetStatement(ptrToRecvData, ptrToHash);
  }

  public void toAssignmentStatements(StatementBuffer assignments) {
    //Change refs
    ptrToRecvData = ptrToRecvData.changeReference(Function.getVars());
    ptrToHash = ptrToHash.changeReference(Function.getVars());
    
    toAssignmentStatements_NoChangeRef(assignments);
  }
  public void toAssignmentStatements_NoChangeRef(StatementBuffer assignments) {
    //Data to put is resolved first
    bitsHash = BitStringHelper.toBitStringFromPtr(ptrToHash, assignments);
    bitsHash.addReference();
    
    //Then receiver
    BitStringHelper.BitStringStoreTarget bsst = BitStringHelper.toBitStringStoreTarget(ptrToRecvData);
    this.bitsToRecvDat = bsst.targetBits;
    LvalExpression target = bsst.targetCVar;
    subStatements = bsst.dummyAssignmentsToTargetBits;
    
    assignments.add(this);
        
    //If we were passed a pointer to target, assign to target.
    if (target != null){
      bitsToRecvDat.toAssignments(target, assignments);
    }
  }  

  public int getOutputLine() {
    return subStatements[0].getOutputLine();
  }
  
  public void toCircuit(Object obj, PrintWriter circuit) {
    circuit.print(getOutputLine()+" "+CBuiltinFunctions.HASHGET_NAME+" ");
    circuit.print("inputs [ NUM_HASH_BITS ");
    IntConstant.valueOf(bitsHash.size()).toCircuit(null, circuit);
    circuit.print(" HASH_IN ");
    //Write the hash bits
    bitsHash.toCircuit(null, circuit);
    //Write the bits to receive the data
    circuit.print(" NUM_Y ");
    IntConstant.valueOf(bitsToRecvDat.size()).toCircuit(null, circuit);
    circuit.print(" Y ");
    bitsToRecvDat.toCircuit(null, circuit);
    circuit.print(" ]\t//");
    circuit.print(subStatements[0].getLHS().getName()+" uint bits 1");
    circuit.println();
  }
}