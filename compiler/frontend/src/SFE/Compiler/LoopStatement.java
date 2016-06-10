package SFE.Compiler;

import java.io.PrintWriter;

public class LoopStatement extends Statement {
  //Compile-time derivable condition.
  private Expression cond;
  private Statement block;

  public LoopStatement(Expression cond, Statement block) {
    this.cond = cond;
    this.block = block;
  }

  public Statement toSLPTCircuit(Object obj) {
    BlockStatement result = new BlockStatement();

    block = block.toSLPTCircuit(null);

    result.addStatement(this.duplicate()); //break ties

    return result;
  }

  public Statement duplicate() {
    LoopStatement neu = new LoopStatement(cond.duplicate(), block.duplicate());
    return neu;
  }

  private static int loop_uid = 0;
  private static int loop_recursion = 0;
  public void toAssignmentStatements(StatementBuffer assignments) {
    while(true){
      //Test the condition.
      cond = cond.changeReference(Function.getVars());
      FloatConstant condValue = FloatConstant.toFloatConstant(cond);
      if (condValue == null){
        //Uncertain (either break or continue)
        loop_recursion++;
        if (loop_recursion > 1000){
          throw new RuntimeException("Infinite recursion possible; condition does not seem to be compile-time resolvable: "+cond);
        }
        BlockStatement doAndLoop = new BlockStatement();
        doAndLoop.addStatement(block.duplicate());
        doAndLoop.addStatement(this.duplicate());
        IfStatement uncertainContinue = new IfStatement(cond.duplicate(), doAndLoop, new BlockStatement(), ":conditioncode"+(loop_uid++));
        uncertainContinue.toSLPTCircuit(null).toAssignmentStatements(assignments);
        loop_recursion--;
        return;
      } else{
        boolean enter = !condValue.isZero();
        if (!enter){
          //Certain break
          break;
        } else {
          //Certain continue
          block.duplicate().toAssignmentStatements(assignments);
          continue;
        }
      }
    }
  }
}
