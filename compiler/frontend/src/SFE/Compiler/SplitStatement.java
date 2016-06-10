package SFE.Compiler;

import java.io.PrintWriter;

import SFE.Compiler.Operators.UnaryPlusOperator;

/**
 * A special kind of assignment statement with multiple LHS's.
 * <lhs1, lhs2, lhs3, ... lhsn> = <bit0, bit1, bit2, ... bitn> of an lval with an integer value.
 */
public class SplitStatement extends StatementWithOutputLine implements OutputWriter{
  private Expression toSplit;
  private AssignmentStatement[] subStatements;
  private Type bitwiseEncoding;
  public SplitStatement(Type bitwiseEncoding, Expression toSplit, LvalExpression[] splitBits){
    this.toSplit = toSplit;
    this.bitwiseEncoding = bitwiseEncoding;
    int N = splitBits.length;
    this.subStatements = new AssignmentStatement[N];
    for(int i = 0; i < N; i++){
      subStatements[i] = new AssignmentStatement(splitBits[i], new Expression[0]);
    }
  }
  public Statement toSLPTCircuit(Object obj) {
    if (toSplit.size() != 1){
      throw new RuntimeException("Cannot split expression: "+toSplit);
    }
    toSplit = toSplit.fieldEltAt(0);
    for(int i = 0; i < subStatements.length; i++){
      subStatements[i] = new AssignmentStatement(subStatements[i].getLHS().fieldEltAt(0),new Expression[0]);
    }
    return this;
  }
  public int getOutputLine() {
    return subStatements[0].getOutputLine();
  }
  public Statement duplicate() {
    LvalExpression[] splitBits = new LvalExpression[subStatements.length];
    for(int i = 0; i < splitBits.length; i++){
      splitBits[i] = subStatements[i].getLHS();
    }
    return new SplitStatement(bitwiseEncoding, toSplit, splitBits);
  }
  public void toAssignmentStatements(StatementBuffer assignments) {
    //Change refs
    toSplit = toSplit.changeReference(Function.getVars());
    toAssignmentStatements_NoChangeRef(assignments);
  }
  public void toAssignmentStatements_NoChangeRef(StatementBuffer assignments) {
    for(int i = 0; i < subStatements.length; i++){
      subStatements[i].setOutputLine(Program.getLineNumber());
    }
     
    IntConstant ic = IntConstant.toIntConstant(toSplit);
    LvalExpression lval = LvalExpression.toLvalExpression(toSplit);
    
    if (ic != null){
      //We can break up the splitStatement, because the toSplit is a constant.
      BitString bc = BitString.toBitString(bitwiseEncoding, null, ic);
      for(int i = 0; i < subStatements.length; i++){
        subStatements[i].addAlternativeRHS(new UnaryOpExpression(new UnaryPlusOperator(), bc.fieldEltAt(i)));
        //add the subStatement directly to the buffer (no change refs.)
        if (Optimizer.isFirstPass()){
          subStatements[i].toAssignmentStatements_NoChangeRef(assignments);
        } else {
          assignments.add(subStatements[i]);
        }
      }
    } else if (lval != null){
      //Search for a bitString for lval, by inlining
      BitString bs = lval.getInlinedBitString();
      if (bs != null){
        if (bs.size() > subStatements.length){
          //This is actually not an error. There is no requirement that a variable's bitstring is minimal
          //in length (and for many bitwise operations, its easier to just return a vector that's too long)
          //so we actually truncate here - but this is safe assuming bitwiseEncoding is correct.
          //throw new RuntimeException("Cannot split "+toSplit+" to "+subStatements.length+" bits, its bitString is "+toSplit);
        }
        for(int i = 0; i < subStatements.length; i++){
          Expression subExpr = IntConstant.ZERO;
          if (i < bs.size()){
            subExpr = bs.fieldEltAt(i);
          }
          subStatements[i].addAlternativeRHS(bs.fieldEltAt(i));
          //add the subStatement directly to the buffer (no change refs.)
          if (Optimizer.isFirstPass()){
            subStatements[i].toAssignmentStatements_NoChangeRef(assignments);
          } else {
            assignments.add(subStatements[i]);
          }
        }
      } else {
        //Inline lval to an lval...
        toSplit = lval;
        //Reference the toSplit
        lval.addReference();
        //Don't add subStatements, instead add this.
        assignments.add(this);
      }
    } else {
      throw new RuntimeException("Cannot split expression: "+toSplit);
    }
  }
  
  public void toCircuit(Object obj, PrintWriter circuit) {
    circuit.print(getOutputLine()+" split ");
    for(AssignmentStatement q : this.subStatements){
      q.getLHS().toCircuit(obj, circuit);
      circuit.print(" ");
    }
    circuit.print("inputs [ ");
    ((OutputWriter)toSplit).toCircuit(obj, circuit);
    circuit.print(" ]\t//");
    circuit.print(subStatements[0].getLHS().getName()+" "+bitwiseEncoding);
    circuit.println();
  }
}
