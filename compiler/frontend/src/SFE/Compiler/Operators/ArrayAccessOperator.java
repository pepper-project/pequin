package SFE.Compiler.Operators;

import SFE.Compiler.AnyType;
import SFE.Compiler.ArrayType;
import SFE.Compiler.CompileTimeOperator;
import SFE.Compiler.Expression;
import SFE.Compiler.Function;
import SFE.Compiler.IntConstant;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;

public class ArrayAccessOperator extends Operator implements
    CompileTimeOperator {
	public int arity() {
		return 2;
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

	public Expression inlineOp(StatementBuffer sb, Expression... args) {
		// Force this expression to resolve.
		return resolve(args[0]);
	}

	public LvalExpression resolve(Expression... args) {
		LvalExpression a = LvalExpression.toLvalExpression(args[0]);

		IntConstant indexConst = IntConstant.toIntConstant(args[1]);
		if (indexConst != null) {

			int index = indexConst.toInt();

			// Check bounds.
			if (!(a.getType() instanceof ArrayType)) {
				throw new RuntimeException("Cannot perform array access on value " + a
				    + " with type " + a.getType());
			}
			int len = ((ArrayType) a.getType()).getLength();
			if (index < 0 || index >= len) {
				throw new RuntimeException("Cannot perform array access on array " + a
				    + " with length " + len + " and index " + index);
			}

			return Function.getVars().getVar(a.getName() + "[" + index + "]")
			    .changeReference(Function.getVars());
		} else {
			throw new RuntimeException("compile time non-resolvable pointer not implemented.");
		}
	}
}
