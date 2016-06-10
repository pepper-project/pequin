package SFE.Compiler.Operators;

import SFE.Compiler.AssignmentStatement;
import SFE.Compiler.BinaryOpExpression;
import SFE.Compiler.BlockStatement;
import SFE.Compiler.Expression;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.SLPTReduction;
import SFE.Compiler.Statement;
import SFE.Compiler.UnaryOpExpression;

/**
 * Just an abstract class extending Operator that implements the default
 * toSLPTExpressionCircuit method.
 */
public abstract class ScalarOperator extends Operator implements SLPTReduction {
	public Statement toSLPTCircuit(Object obj) {
		AssignmentStatement as = ((AssignmentStatement) obj);
		LvalExpression lhs = as.getLHS(); // LHS of the param statement

		BlockStatement result = new BlockStatement();
		if (arity() == 2) {
			BinaryOpExpression rhs = (BinaryOpExpression) (as.getRHS());

			Expression right = rhs.getRight();

			Expression left = rhs.getLeft();

			result.addStatement(new AssignmentStatement(lhs.lvalFieldEltAt(0),
			    new BinaryOpExpression(this, left, right)));

		} else if (arity() == 1) {
			UnaryOpExpression rhs = (UnaryOpExpression) (as.getRHS());

			Expression middle = rhs.getMiddle();
			middle = middle.evaluateExpression(lhs.getName(), "M", result); // produces
																																			// SLPT
																																			// circuit

			result.addStatement(new AssignmentStatement(lhs.lvalFieldEltAt(0),
			    new UnaryOpExpression(this, middle)));
		} else {
			throw new RuntimeException(
			    "I don't know how to SLPTReduce an operator with arity " + arity()
			        + " " + this);
		}
		return result;
	}

	public Expression fieldEltAt(Expression expression, int i) {
		if (i != 0) {
			throw new RuntimeException("Array index out of bounds exception index "
			    + i + " fieldEltAt a scalar operator " + this);
		}
		return expression;
	}
}
