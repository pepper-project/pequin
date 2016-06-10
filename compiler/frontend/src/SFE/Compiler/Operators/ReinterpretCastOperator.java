package SFE.Compiler.Operators;

import java.io.PrintWriter;

import SFE.Compiler.AssignmentStatement;
import SFE.Compiler.BlockStatement;
import SFE.Compiler.Expression;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.OutputWriter;
import SFE.Compiler.SLPTReduction;
import SFE.Compiler.Statement;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;
import SFE.Compiler.UnaryOpExpression;


public class ReinterpretCastOperator extends ScalarOperator implements SLPTReduction, OutputWriter {
  private Type targetType;

  public ReinterpretCastOperator(Type targetType) {
    if (targetType.size() > 1) {
      throw new RuntimeException("Currently can only do reinterpret casts of primitive types (float, int, etc.)");
    }
    this.targetType = targetType;
  }

  public int arity() {
    return 1;
  }

  public int priority() {
    throw new RuntimeException("Not implemented");
  }

  public Type getType(Object obj) {
    return targetType;
  }

  public Expression inlineOp(StatementBuffer sb, Expression ... args) {
    Expression middle = args[0];
    return new UnaryOpExpression(this, middle);
  }

  public void toCircuit(Object obj, PrintWriter circuit) {
    UnaryOpExpression expr = (UnaryOpExpression)obj;
    ((OutputWriter)expr.getMiddle()).toCircuit(null, circuit);
  }

  public String toString() {
    return "poly";
  }

  public Expression resolve(Expression... args) {
    return null;
  }
}
