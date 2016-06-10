package SFE.Compiler;

import java.io.PrintWriter;

import ccomp.CBuiltinFunctions;

/**
 * A statement which turns in a hash and receives the object the database has for that hash
 */
public class HashFreeStatement extends StatementWithOutputLine implements OutputWriter{
  private Expression ptrToHash;
  private BitString bitsHash; //Filled in during toAssignmentStatements  
  private int outputLine;
  
  /**
   * Standard - expressions can be pointers to data, or a bitstring.
   */
  public HashFreeStatement(Expression pointerToHash) {
    this.ptrToHash = pointerToHash;
  }

  public Statement toSLPTCircuit(Object obj) {
    if (!(ptrToHash instanceof BitString)){
      ptrToHash = ptrToHash; //.bitAt(0);
    }
    return this; 
  }

  public Statement duplicate() {
    return new HashFreeStatement(ptrToHash);
  }

  public void toAssignmentStatements(StatementBuffer assignments) {
    //Change refs
    ptrToHash = ptrToHash.changeReference(Function.getVars());
    
    toAssignmentStatements_NoChangeRef(assignments);
  }
  public void toAssignmentStatements_NoChangeRef(StatementBuffer assignments) {
    //Data to put is resolved first
    bitsHash = BitStringHelper.toBitStringFromPtr(ptrToHash, assignments);
    bitsHash.addReference();

    //Get the next available number in the program.
    outputLine = Program.getLineNumber();
    
    assignments.add(this);
  }  

  public int getOutputLine() {
    return outputLine;
  }
  
  public void toCircuit(Object obj, PrintWriter circuit) {
    circuit.print(getOutputLine()+" "+CBuiltinFunctions.HASHFREE_NAME+" ");
    circuit.print("inputs [ NUM_HASH_BITS ");
    IntConstant.valueOf(bitsHash.size()).toCircuit(null, circuit);
    circuit.print(" HASH_IN ");
    //Write the hash bits
    bitsHash.toCircuit(null, circuit);
    circuit.print(" ]\t//");
    circuit.print("void uint bits 1");
    circuit.println();
  }
}