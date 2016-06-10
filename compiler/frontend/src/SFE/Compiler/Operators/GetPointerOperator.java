package SFE.Compiler.Operators;

import SFE.Compiler.AnyType;
import SFE.Compiler.AssignmentStatement;
import SFE.Compiler.Expression;
import SFE.Compiler.IntConstant;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.Pointer;
import SFE.Compiler.SLPTReduction;
import SFE.Compiler.Statement;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;
import SFE.Compiler.UnaryOpExpression;
import SFE.Compiler.UnaryOperator;

/**
 * This corresponds to & operator in C
 * 
 */
public class GetPointerOperator extends Operator implements UnaryOperator,
    SLPTReduction {
	public int arity() {
		return 1;
	}

	public int priority() {
		throw new RuntimeException("Not implemented");
	}

	public Type getType(Object obj) {
		// Not determinate type -> should return AnyType.
		return new AnyType();
	}

	public Expression fieldEltAt(Expression expression, int i) {
		throw new RuntimeException("Not yet implemented");
	}

	public Expression inlineOp(StatementBuffer assignments, Expression... args) {
		return resolve(args);
	}

	public Pointer resolve(Expression... args) {
		LvalExpression a = LvalExpression.toLvalExpression(args[0]);
		if (a != null) {
			int location = Pointer.getMemoryLocation(a);
			return new Pointer(a.getType(), new LvalExpression[] { a }, location,
			    location);
		}
		// throw new RuntimeException("not implemented yet.");
		return null;
	}

	@Override
  public Statement toSLPTCircuit(Object obj) {
		// if variable, get its address
		// otherwise see if its GetPoitner of a PointerAccess operator.
		AssignmentStatement as = (AssignmentStatement) obj;
		UnaryOpExpression rhs = (UnaryOpExpression) as.getRHS();

    Expression middle = rhs.getMiddle();
    if (middle instanceof UnaryOpExpression) {
    	UnaryOpExpression uoe = (UnaryOpExpression) middle;
    	if (uoe.getOperator() instanceof PointerAccessOperator) {
    		Expression address = uoe.getMiddle();
    	  AssignmentStatement newAs = new AssignmentStatement(as.getLHS(), address);
    	  return newAs;
    	}
    } else if (middle instanceof LvalExpression) {
    	LvalExpression le = (LvalExpression)middle;
    	AssignmentStatement newAs = new AssignmentStatement(as.getLHS(), IntConstant.valueOf(le.getAddress()));
    	return newAs;
    }
    throw new RuntimeException("I'm not sure how to handle this.");
  }
}
