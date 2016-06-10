package SFE.Compiler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import ccomp.CBuiltinFunctions;

/**
 * A statement which writes to the database.
 */
public class RamPutStatement extends StatementWithOutputLine implements OutputWriter{
  private LvalExpression[] address;
  private Expression ptrToDataToPut;
  private BitString bitsToPut; //Filled in during toAssignmentStatements  
  
  private int outputLine;
  
  private static LvalExpression[] col2arr(Collection<LvalExpression> a){
    LvalExpression[] toRet = new LvalExpression[a.size()];
    int p = 0;
    for(LvalExpression q : a){
      toRet[p++]= q;
    }
    return toRet;
  }
  /**
   * data can either resolve to a pointer to data or data can resolve to a bitstring.
   * 
   * In the first case, the pointer to data is resolved and a new bitstring is made.
   */
  public RamPutStatement(Collection<LvalExpression> addrs, Expression data) {
    this.ptrToDataToPut = data;
    this.address = col2arr(addrs);
  }

  public Statement toSLPTCircuit(Object obj) {
    for(int i = 0; i < address.length; i++){
      address[i] = address[i].fieldEltAt(0);
    }
    if (!(ptrToDataToPut instanceof BitString)){
      ptrToDataToPut = ptrToDataToPut.fieldEltAt(0);
    }
    
    return this; 
  }

  public Statement duplicate() {
    ArrayList<LvalExpression> naddr = new ArrayList();
    for(LvalExpression q : address){
      naddr.add(q);
    }
    return new RamPutStatement(naddr, ptrToDataToPut);
  }

  public void toAssignmentStatements(StatementBuffer assignments) {
    //Change refs, and reference them
    for(int i = 0; i < address.length; i++){
      address[i] = address[i].changeReference(Function.getVars());
    }
    ptrToDataToPut = ptrToDataToPut.changeReference(Function.getVars());
    
    toAssignmentStatements_NoChangeRef(assignments);
  }
  public void toAssignmentStatements_NoChangeRef(StatementBuffer assignments) {
    for(int i = 0; i < address.length; i++){
      address[i].addReference();
    }
    
    //OK. value is a pointer
    if (ptrToDataToPut instanceof BitString){
      bitsToPut = (BitString)ptrToDataToPut;
    } else {
      Pointer ptr = Pointer.toPointerConstant(ptrToDataToPut);
      LvalExpression actualData = ptr.access();
      //Turn into a bit string of that many bits.
      //Get all derived bits and split them up.
      Type t = actualData.getDeclaredType();
      bitsToPut = BitString.toBitString(t, assignments, actualData);
    };
    bitsToPut.addReference();
    
    outputLine = Program.getLineNumber();
    assignments.add(this);
  }  

  public int getOutputLine() {
    return outputLine;
  }
  
  public void toCircuit(Object obj, PrintWriter circuit) {
    circuit.print(getOutputLine()+" "+CBuiltinFunctions.RAMPUT_NAME+" ");
    circuit.print("inputs [ ADDR ");
    for(LvalExpression q : this.address){
      q.toCircuit(null, circuit);
      circuit.print(" ");
    }
    circuit.print("NUM_X ");
    IntConstant.valueOf(bitsToPut.size()).toCircuit(null, circuit);
    circuit.print(" X ");
    bitsToPut.toCircuit(null, circuit);
    circuit.print(" ]\t//");
    circuit.print("void");
    circuit.println();
  }
}