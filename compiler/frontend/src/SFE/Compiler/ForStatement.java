package SFE.Compiler;

import java.io.PrintWriter;
import java.math.BigInteger;

import SFE.Compiler.Operators.UnaryPlusOperator;



public class ForStatement extends Statement {
  private LvalExpression var;
  private Expression from;
  private Expression to;
  private Statement forBlock;
  //Used for making the variables from and to have unique names
  private String fromName;
  private String toName;

  public ForStatement(LvalExpression var, Expression from, Expression to, Statement forBlock, String fromName, String toName) {
    this.var = var;
    this.from = from;
    this.to = to;
    this.forBlock = forBlock;
    this.fromName = fromName;
    this.toName = toName;
  }

  public Statement toSLPTCircuit(Object obj) {
    BlockStatement result = new BlockStatement();

    // create a temp var that holds the from expression
    LvalExpression from_var = Function.addTempLocalVar("$from" + fromName, new FloatType());

    // create the assignment statement that assings the result
    if (!(from instanceof OperationExpression)) {
      from = new UnaryOpExpression(new UnaryPlusOperator(), from);
    }

    AssignmentStatement fromAs = new AssignmentStatement(from_var, (OperationExpression) from);

    result.addStatement(fromAs.toSLPTCircuit(null));

    from = from_var;//.bitAt(0);

    // create a temp var that holds the to expression
    LvalExpression to_var = Function.addTempLocalVar("$to" + toName, new FloatType());

    // create the assignment statement that assings the result
    if (!(to instanceof OperationExpression)) {
      to = new UnaryOpExpression(new UnaryPlusOperator(), to);
    }

    AssignmentStatement toAs = new AssignmentStatement(to_var, (OperationExpression) to);

    result.addStatement(toAs.toSLPTCircuit(null));

    to = to_var; //.bitAt(0);

    forBlock = forBlock.toSLPTCircuit(obj);

    result.addStatement(this.duplicate()); //break ties

    return result;
  }

  public Statement duplicate() {
    ForStatement neu = new ForStatement(var, from.duplicate(), to.duplicate(), forBlock.duplicate(), fromName, toName);
    return neu;
  }

  public void toAssignmentStatements(StatementBuffer assignments) {
    from = Function.getVar((LvalExpression)from);
    to = Function.getVar((LvalExpression)to);

    BigInteger from = IntConstant.toIntConstant(this.from).value();
    BigInteger to = IntConstant.toIntConstant(this.to).value();

    for(BigInteger i = from; i.compareTo(to) <= 0; i = i.add(BigInteger.ONE)) {
      AssignmentStatement as = new AssignmentStatement(var, new UnaryOpExpression(new UnaryPlusOperator(),
          IntConstant.valueOf(i)));
      as.toSLPTCircuit(null).toAssignmentStatements(assignments);

      forBlock.duplicate().toAssignmentStatements(assignments);
    }
  }
}
