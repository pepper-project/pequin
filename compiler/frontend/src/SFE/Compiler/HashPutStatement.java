package SFE.Compiler;

import java.io.PrintWriter;

import ccomp.CBuiltinFunctions;

/**
 * A statement which turns in an object and receives a hash for the object.
 */
public class HashPutStatement extends StatementWithOutputLine implements OutputWriter{
  private Expression ptrToDataToPut;
  private Expression ptrToRecvHash;
  private BitString bitsToPut; //Filled in during toAssignmentStatements  
  private BitString bitsToRecvHash; //Filled in during toAssignmentStatements
  private AssignmentStatement[] subStatements; //Filled in during toAssignmentStatements
  
  /**
   * data can either resolve to a pointer to data or data can resolve to a bitstring.
   * This also holds for pointerToReceiveHash.
   * 
   * In the first case, the pointer to data is resolved and a new bitstring is made.
   */
  public HashPutStatement(Expression pointerToReceiveHash, Expression data) {
    this.ptrToDataToPut = data;
    this.ptrToRecvHash = pointerToReceiveHash;
  }

  public Statement toSLPTCircuit(Object obj) {
    if (!(ptrToDataToPut instanceof BitString)){
      ptrToDataToPut = ptrToDataToPut;//.bitAt(0);
    }
    if (!(ptrToRecvHash instanceof BitString)){
      ptrToRecvHash = ptrToRecvHash;//.bitAt(0);
    }
    return this; 
  }

  public Statement duplicate() {
    return new HashPutStatement(ptrToRecvHash, ptrToDataToPut);
  }

  public void toAssignmentStatements(StatementBuffer assignments) {
    //Change refs, and reference them
    ptrToRecvHash = ptrToRecvHash.changeReference(Function.getVars());
    ptrToDataToPut = ptrToDataToPut.changeReference(Function.getVars());
    
    toAssignmentStatements_NoChangeRef(assignments);
  }
  public void toAssignmentStatements_NoChangeRef(StatementBuffer assignments) {
    //Data to put is expanded first
    bitsToPut = BitStringHelper.toBitStringFromPtr(ptrToDataToPut, assignments);
    bitsToPut.addReference();

    //Then receiver.
    BitStringHelper.BitStringStoreTarget bsst = BitStringHelper.toBitStringStoreTarget(ptrToRecvHash);
    this.bitsToRecvHash = bsst.targetBits;
    LvalExpression hashTarget = bsst.targetCVar;
    subStatements = bsst.dummyAssignmentsToTargetBits;     
    
    assignments.add(this);
        
    //If we were passed a pointer to target, assign to target.
    if (hashTarget != null){
      bitsToRecvHash.toAssignments(hashTarget, assignments);
    }
  }  

  public int getOutputLine() {
    return subStatements[0].getOutputLine();
  }
  
  public void toCircuit(Object obj, PrintWriter circuit) {
    circuit.print(getOutputLine()+" "+CBuiltinFunctions.HASHPUT_NAME+" ");
    circuit.print("inputs [ NUM_HASH_BITS ");
    IntConstant.valueOf(bitsToRecvHash.size()).toCircuit(null, circuit);
    circuit.print(" HASH_OUT ");
    //Write the name of the first target hash bit (the rest are sequential)
    bitsToRecvHash.toCircuit(null, circuit);
    //Write the input to be hashed
    circuit.print(" NUM_X ");
    IntConstant.valueOf(bitsToPut.size()).toCircuit(null, circuit);
    circuit.print(" X ");
    bitsToPut.toCircuit(null, circuit);
    circuit.print(" ]\t//");
    circuit.print(subStatements[0].getLHS().getName()+" uint bits 1");
    circuit.println();
  }
}