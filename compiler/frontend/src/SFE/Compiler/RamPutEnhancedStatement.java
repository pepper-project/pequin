package SFE.Compiler;

import java.io.PrintWriter;

import SFE.Compiler.Operators.PlusOperator;
import ccomp.CBuiltinFunctions;

/**
 * A statement which writes to the database.
 */
public class RamPutEnhancedStatement extends StatementWithOutputLine implements
		OutputWriter {
	private Expression address;
	private Expression value;
	private LvalExpression returnVar;

	private Expression condition;
	private boolean branch;

	private AssignmentStatement dummyAs;
	private int outputLine;

	/**
	 * data can either resolve to a pointer to data or data can resolve to a
	 * bitstring.
	 * 
	 * In the first case, the pointer to data is resolved and a new bitstring is
	 * made.
	 */
	public RamPutEnhancedStatement(Expression addr, Expression value) {
		this.value = value;
		this.address = addr;
	}

	public RamPutEnhancedStatement(Expression addr, Expression value,
			Expression condition, boolean branch) {
		this.value = value;
		this.address = addr;
		this.condition = condition;
		this.branch = branch;
	}

	public RamPutEnhancedStatement(Expression addr, Expression value,
			Expression condition, boolean branch, LvalExpression returnVar) {
		this.value = value;
		this.address = addr;
		this.condition = condition;
		this.branch = branch;
		this.returnVar = returnVar;
	}

	public static int addressCounter = 0;
	public static int returnVarCounter = 0;

	public Statement toSLPTCircuit(Object obj) {
		BlockStatement result = new BlockStatement();

		// evaluate address, so that address is an lvalue.
		Expression newAddress = null;
		if (value instanceof LvalExpression) {
			newAddress = address.evaluateExpression(
					((LvalExpression) value).getName(), "addr", result);
		} else {
			newAddress = address.evaluateExpression("ramput", "addr"
					+ addressCounter, result);
			addressCounter++;
		}
		if (newAddress != null) {
			address = newAddress;
		} else {
			throw new RuntimeException(
					"I don't know how to evaluate the address of the ram operation.");
		}

		// if the value's size is greater than 1, expand the ramget into
		// multiple.

		if (value.size() > 1) {
			if (value instanceof LvalExpression) {
				for (int i = 0; i < value.size(); i++) {
					LvalExpression lval = (LvalExpression) value.fieldEltAt(i);
					Expression fieldAddr = new BinaryOpExpression(
							new PlusOperator(), address, IntConstant.valueOf(i));
					Statement ramput = new RamPutEnhancedStatement(fieldAddr,
							lval);
					ramput = ramput.toSLPTCircuit(null);
					result.addStatement(ramput);
				}
			} else {
				throw new RuntimeException(
						"I don't know how to handle non-lvalue whose size is greater than 1.");
			}
		} else {
			result.addStatement(this);
		}
		return result;
	}

	public Statement duplicate() {
		return new RamPutEnhancedStatement(address, value, condition, branch);
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
		if (value instanceof LvalExpression) {
			((LvalExpression) value).addReference();
		}

		if (returnVar == null) {
			returnVar = Function.getVars()
					.addVar("returnVar" + returnVarCounter, new BusType(),
							false, false);
			returnVarCounter++;

			dummyAs = new AssignmentStatement(returnVar, new Expression[0]);
			dummyAs.setOutputLine(Program.getLineNumber());
			assignments.add(this);
		} else {
			dummyAs = new AssignmentStatement((LvalExpression) returnVar,
					new Expression[0]);
			dummyAs.setOutputLine(Program.getLineNumber());
			assignments.add(this);
		}

		if (condition == null) {
			condition = assignments.getCondition();
			branch = assignments.getBranch();

			if (condition == null) {
				// the ram operation always be executed...
				condition = IntConstant.ONE;
				branch = true;
			}
		}

		if (condition instanceof LvalExpression) {
			((LvalExpression) condition).addReference();
		}

		outputLine = Program.getLineNumber();
		assignments.add(this);
	}

	public int getOutputLine() {
		return dummyAs.getOutputLine();
	}

	public void toCircuit(Object obj, PrintWriter circuit) {
		circuit.print(getOutputLine() + " "
				+ CBuiltinFunctions.RAMPUT_ENHANCED_NAME + " ");

		returnVar.toCircuit(null, circuit);

		circuit.print(" inputs [ ADDR ");
		((OutputWriter) address).toCircuit(null, circuit);
		circuit.print(" ");
		circuit.print("VALUE ");
		((OutputWriter) value).toCircuit(null, circuit);
		circuit.print(" CONDITION ");
		if (condition instanceof LvalExpression) {
			// condition is a variable.
			((LvalExpression) condition).toCircuit(null, circuit);
		} else {
			// condition is a constant.
			if (condition instanceof IntConstant) {
				((IntConstant) condition).toCircuit(null, circuit);
			} else if (condition instanceof FloatConstant) {
				((FloatConstant) condition).toCircuit(null, circuit);
			}
		}
		circuit.print(" " + branch);
		circuit.print(" ]\t//");
		circuit.print("ramput " + value.getType());
		circuit.println();
	}

	@Override
	public String toString() {
		return "ramput_fast(" + address + "," + value + ")";
	}
}
