package SFE.Compiler;

import java.io.PrintWriter;

import ccomp.CBuiltinFunctions;

/**
 * A statement which turns in a hash and performs some sort of data retrieval from the prover's state
 */
public class GenericGetStatement extends StatementWithOutputLine implements OutputWriter{
  //An identifier string used in the .circuit file
  public static final String GENERIC_GET_STATEMENT_STR = "genericget";
  
  //Given a commitment hash, retrieve the associated commitment (retrieved data includes private key needed to verify retrieval) 
  public static final String COMMITMENT_LOOKUP = "commitment";
  
  //The kind of lookup to perform.
  private String lookup_type;
  private Expression ptrToRecvData;
  private Expression ptrToHash;
  private BitString bitsHash; //Filled in during toAssignmentStatements  
  private BitString bitsToRecvDat; //Filled in during toAssignmentStatements
  private AssignmentStatement[] subStatements; //Filled in during toAssignmentStatements
  
  /**
   * Standard - expressions can be pointers to data, or a bitstring.
   */
  public GenericGetStatement(String lookup_type, Expression pointerToRecvData, Expression pointerToHash) {
    this.lookup_type = lookup_type;
    this.ptrToRecvData = pointerToRecvData;
    this.ptrToHash = pointerToHash;
  }

  public Statement toSLPTCircuit(Object obj) {
    return this; 
  }

  public Statement duplicate() {
    throw new RuntimeException("Not yet implemented");
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
    circuit.print(getOutputLine()+" "+GENERIC_GET_STATEMENT_STR+" "+lookup_type+" ");
    circuit.print("inputs [ ");
    circuit.print("NUM_HASH_BITS ");
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