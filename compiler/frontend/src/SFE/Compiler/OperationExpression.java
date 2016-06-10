// OperationExpression.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import SFE.Compiler.Operators.GetPointerOperator;
import SFE.Compiler.Operators.Operator;
import SFE.Compiler.Operators.PointerAccessOperator;
import SFE.Compiler.Operators.StructAccessOperator;
import SFE.Compiler.Operators.UnaryPlusOperator;

/**
 * class OperationExpression defines expressions containing operatioins, that
 * can be defined in the program.
 */
public abstract class OperationExpression extends Expression implements
    SLPTReduction, OutputWriter {
	// ~ Instance fields --------------------------------------------------------

	/*
	 * Holds the operator of this expression
	 */
	public Operator op;

	// ~ Constructors -----------------------------------------------------------

	/**
	 * Constracts a new OperationExpression from a given Operator.
	 * 
	 * @param op
	 *          this OperationExpression operator.
	 */
	public OperationExpression(Operator op) {
		this.op = op;
	}

	// ~ Methods ----------------------------------------------------------------

	public Operator getOperator() {
		return op;
	}

	/**
	 * Sorts the input gates according to their names and returns the result
	 * OperationExpression. This method is used in the optimization process.
	 * (Right is the smallest, left the biggest)
	 * 
	 * @return the OperationExpression with the sorted inputs.
	 */
	public abstract OperationExpression sortInputs();

	/**
	 * recursively calculates inner arithmetic expression to a scalar type in an
	 * lvalue
	 * 
	 * @param as
	 *          the AssignmentStatement that is associated with this expression
	 *          (note - used for naming purposes only)
	 * @param result
	 *          a block statement to insert statments if needed.
	 * @param size
	 *          - the size of the temporary variable (must be known before hand)
	 * @return the new statement to use instead of as.
	 */
	public Expression evaluateExpression(String goal, String tempName,
	    BlockStatement result) {
		if (op instanceof CompileTimeOperator) {
			return this;
		}

		// For the unary plus operator, just fall through.
		if (op instanceof UnaryPlusOperator) {
			return ((UnaryOpExpression) this).getMiddle().evaluateExpression(goal,
			    tempName, result);
		}

		if (op instanceof StructAccessOperator) {
			// try to resolve compile-time struct access a little bit earlier.
			LvalExpression lval = LvalExpression.toLvalExpression(this);
			if (lval != null) {
				// call evaluateExpression so that a proper ramget can be generated if
				// necessary.
				// this makes sure y = *x + b and y = x->a + b compiles correctly.
				return lval.evaluateExpression(goal, tempName, result);
			}
		}

		if (op instanceof PointerAccessOperator) {
			// try to resolve compile-time pointer access a little bit earlier.
			LvalExpression lval = LvalExpression.toLvalExpression(this);
			if (lval != null) {
				// call evaluateExpression so that a proper ramget can be generated if
				// necessary.
				// this makes sure y = *x + b and y = x->a + b compiles correctly.
				return lval.evaluateExpression(goal, tempName, result);
			}
		}

		if (op instanceof GetPointerOperator) {
			return this;
		}

		LvalExpression tmpLvalExp = Function.addTempLocalVar(goal
		    + Expression.TEMPORARY_SEPARATOR + tempName /* + tempLabel */,
		    new BusType());
		// create the assignment statement
		AssignmentStatement tempAs = new AssignmentStatement(tmpLvalExp,
		    (OperationExpression) this);

		// evaluate the expression and store it in the tmp lval expression
		result.addStatement(tempAs.toSLPTCircuit(null));
		tmpLvalExp.metaType = this.metaType;

		/*
		 * tempLabel++; if (tempLabel > 1000){ System.out.println(this); }
		 */

		return tmpLvalExp;
	}

	/**
	 * returns a replica of this Expression
	 * 
	 * @return a replica of this Expression
	 */
	public abstract Expression duplicate();

}
