package SFE.Compiler.Operators;

import java.io.PrintWriter;

import SFE.Compiler.ArrayType;
import SFE.Compiler.AssignmentStatement;
import SFE.Compiler.BitString;
import SFE.Compiler.BlockStatement;
import SFE.Compiler.Expression;
import SFE.Compiler.IntType;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.OutputWriter;
import SFE.Compiler.Pointer;
import SFE.Compiler.PointerType;
import SFE.Compiler.SLPTReduction;
import SFE.Compiler.Statement;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;
import SFE.Compiler.TypeHeirarchy;
import SFE.Compiler.UnaryOpExpression;

public class CStyleCast extends Operator implements SLPTReduction, OutputWriter {
	private Type targetType;

	public CStyleCast(Type targetType) {
		if (!(targetType instanceof IntType) || targetType.size() > 1) {
			throw new RuntimeException(
			    "Currently can only do C-style casts to int types");
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

	public Expression fieldEltAt(Expression expression, int i) {
		if (i != 0) {
			throw new RuntimeException("Array index out of bounds exception index "
			    + i + " fieldEltAt casting " + this);
		}
		return expression;
	}

	public Statement toSLPTCircuit(Object obj) {
		AssignmentStatement as = ((AssignmentStatement) obj);
		BlockStatement result = new BlockStatement();
		LvalExpression lhs = as.getLHS();

		UnaryOpExpression rhs = (UnaryOpExpression) as.getRHS();
		Expression middle = rhs.getMiddle();

		AssignmentStatement subAs;
		if (middle.metaType instanceof ArrayType) {
			subAs = new AssignmentStatement(lhs.lvalFieldEltAt(0),
			    new UnaryOpExpression(this, middle));
		} else {
			middle = middle.evaluateExpression(lhs.getName(), "M", result); // produces
																																			// SLPT
																																			// circuit
			subAs = new AssignmentStatement(lhs.lvalFieldEltAt(0),
			    new UnaryOpExpression(this, middle));
		}
		result.addStatement(subAs);

		return result;
	}

	public Expression inlineOp(StatementBuffer sb, Expression... args) {
		Expression in = Expression.fullyResolve(args[0]);
		if (TypeHeirarchy.isSubType(in.getType(), targetType)) {
			// No problem~!
			return in;
		}
		if (targetType instanceof PointerType) {
			// Special case: Convert array to pointer.
			if (in.getType() instanceof ArrayType) {
				Pointer toRet = Pointer.toPointerConstant(in);
				if (!TypeHeirarchy.equals(toRet.getType().getPointedToType(),
				    ((PointerType) targetType).getPointedToType())) {
					throw new RuntimeException("I don't know how to cast " + in
					    + " of type " + in.getType() + " to pointer-to-"
					    + ((PointerType) targetType).getPointedToType());
				}
				if (!TypeHeirarchy.isSubType(toRet.getType(), targetType)) {
					throw new RuntimeException("Assertion error");
				}
				return toRet;
			}
			if (in instanceof LvalExpression) {
				return in;
			}
			throw new RuntimeException("I don't know how to cast " + in + " of type "
			    + in.getType() + " to " + targetType);
		} else {
			// Remaining case: Convert at the bit-string level.
			return BitString.toBitString(targetType, sb, in);
		}
	}

	public void toCircuit(Object obj, PrintWriter circuit) {
		UnaryOpExpression expr = (UnaryOpExpression) obj;
		((OutputWriter) expr.getMiddle()).toCircuit(null, circuit);
	}

	public String toString() {
		return "C-style-cast(" + targetType + ")";
	}

	public Expression resolve(Expression... args) {
		// leave it for inlineOp to take care of.
		return null;
	}
}
