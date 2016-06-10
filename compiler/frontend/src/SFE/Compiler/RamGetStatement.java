package SFE.Compiler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import ccomp.CBuiltinFunctions;

/**
 * A statement which writes to the database.
 */
public class RamGetStatement extends StatementWithOutputLine implements OutputWriter{
  private LvalExpression[] address;
  private Expression ptrToTarget;
  private BitString bitsTarget; //Filled in during toAssignmentStatements
  private AssignmentStatement[] subStatements; //Filled in during toAssignmentStatements
  
  private int outputLine;

  private static LvalExpression[] col2Arr(Collection<LvalExpression> a){
    LvalExpression[] toRet = new LvalExpression[a.size()];
    int p = 0;
    for(LvalExpression q : a){
      toRet[p++]= q;
    }
    return toRet;
  }
  public RamGetStatement(Expression ptrToTarget, Collection<LvalExpression> address) {
    this.ptrToTarget = ptrToTarget;
    this.address = col2Arr(address);
  }

  public Statement toSLPTCircuit(Object obj) {
    for(int i = 0; i < address.length; i++){
      address[i] = address[i].fieldEltAt(0);
    }
    ptrToTarget = ptrToTarget.fieldEltAt(0);
    
    return this; 
  }

  public Statement duplicate() {
    ArrayList<LvalExpression> naddr = new ArrayList();
    for(LvalExpression q : address){
      naddr.add(q);
    }
    return new RamGetStatement(ptrToTarget, naddr);
  }

  public void toAssignmentStatements(StatementBuffer assignments) {
    //Change refs, and reference them
    for(int i = 0; i < address.length; i++){
      address[i] = address[i].changeReference(Function.getVars());
    }
    ptrToTarget = ptrToTarget.changeReference(Function.getVars());
    
    toAssignmentStatements_NoChangeRef(assignments);
  }
  public void toAssignmentStatements_NoChangeRef(StatementBuffer assignments) {
    for(int i = 0; i < address.length; i++){
      address[i].addReference();
    }
    
    //Dereference the store pointer
    BitStringHelper.BitStringStoreTarget bsst = BitStringHelper.toBitStringStoreTarget(ptrToTarget);
    this.bitsTarget = bsst.targetBits;
    LvalExpression target = bsst.targetCVar;
    subStatements = bsst.dummyAssignmentsToTargetBits;
    
    assignments.add(this);
    
    //If we were passed a pointer to target, assign to target.
    if (target != null){
      bitsTarget.toAssignments(target, assignments);
    }
  }

  public int getOutputLine() {
    return subStatements[0].getOutputLine();
  }
  
  public void toCircuit(Object obj, PrintWriter circuit) {
    circuit.print(getOutputLine()+" "+CBuiltinFunctions.RAMGET_NAME+" ");
    circuit.print("inputs [ ADDR ");
    for(LvalExpression q : this.address){
      q.toCircuit(obj, circuit);
      circuit.print(" ");
    }
    circuit.print(" NUM_Y ");
    IntConstant.valueOf(bitsTarget.size()).toCircuit(null, circuit);
    circuit.print(" Y "); 
    bitsTarget.toCircuit(obj, circuit);
    circuit.print(" ]\t//");
    circuit.print(subStatements[0].getLHS().getName()+" uint bits 1");
    circuit.println();
  }
}
