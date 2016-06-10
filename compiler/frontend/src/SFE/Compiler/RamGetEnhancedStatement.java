package SFE.Compiler;

import java.io.PrintWriter;

import SFE.Compiler.Operators.PlusOperator;
import ccomp.CBuiltinFunctions;

/**
 * A statement which writes to the database.
 */
public class RamGetEnhancedStatement extends StatementWithOutputLine implements
    OutputWriter {
	private Expression address;
	private LvalExpression value;

	private int outputLine;

	public RamGetEnhancedStatement(LvalExpression value, Expression address) {
		this.value = value;
		this.address = address;
		this.value.setAssigningStatement(this);
	}

	public Statement toSLPTCircuit(Object obj) {
		BlockStatement result = new BlockStatement();

		// evaluate address, so that address is an lvalue.
		Expression newAddress = address.evaluateExpression(value.getName(), "addr",
		    result);
		if (newAddress != null) {
			address = newAddress;
		} else {
			throw new RuntimeException(
			    "I don't know how to evaluate the address of the ram operation.");
		}

		// if the value's size is greater than 1, expand the ramget into
		// multiple.
		if (value.size() > 1) {
			for (int i = 0; i < value.size(); i++) {
				LvalExpression lval = value.fieldEltAt(i);
				Expression fieldAddr = new BinaryOpExpression(new PlusOperator(),
				    address, IntConstant.valueOf(i));
				Statement ramget = new RamGetEnhancedStatement(lval, fieldAddr);
				ramget = ramget.toSLPTCircuit(null);
				result.addStatement(ramget);
			}
		} else {
			result.addStatement(this);
		}
		return result;
	}

	public Statement duplicate() {
		return new RamGetEnhancedStatement(value, address);
	}

	public void toAssignmentStatements(StatementBuffer assignments) {
		// Change refs, and reference them
		address = address.changeReference(Function.getVars());
		value = value.changeReference(Function.getVars());

		toAssignmentStatements_NoChangeRef(assignments);
	}

	public void toAssignmentStatements_NoChangeRef(StatementBuffer assignments) {
		if (address instanceof LvalExpression) {
			((LvalExpression) address).addReference();
		}

		outputLine = Program.getLineNumber();
		assignments.add(this);
		Statement oldAssignment = null;
		if (Function.getVar(value) != null) {
			oldAssignment = Function.getVar(value).getAssigningStatement();
			// this assignment updates lhs, which invalidates its value got from the the
			// previous assignment.
			// overwrite this reference with this assignment
			
			if (oldAssignment != this) {
				Type type = value.getType();
				Function.addVar(value); // .getName(), lhs.getType(), lhs.isOutput());
				// get the new ref to lhs
				value = Function.getVar(value);
				// this assignment updates lhs, which invalidates its value got from the
				// the previous assignment.
				// overwrite this reference with this assignment
				value.setAssigningStatement(this);
				value.setType(type);
			}
		}
	}

	public int getOutputLine() {
		return outputLine;
	}

	public void toCircuit(Object obj, PrintWriter circuit) {
		circuit.print(getOutputLine() + " "
		    + CBuiltinFunctions.RAMGET_ENHANCED_NAME + " VALUE ");
		value.toCircuit(null, circuit);
		// now starts input
		circuit.print(" inputs [ ADDR ");
		((OutputWriter) address).toCircuit(null, circuit);
		circuit.print(" ]\t//");

		circuit.print(value.getName() + " " + value.getType());
		circuit.println();
	}

	@Override
	public String toString() {
		return "ramget_fast(" + address + ")";
	}
}
