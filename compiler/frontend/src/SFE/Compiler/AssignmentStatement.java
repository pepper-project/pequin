// AssignmentStatement.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Map;

import SFE.Compiler.Operators.UnaryPlusOperator;

/**
 * A class for representing assignment statements that can be defined in the
 * program.
 */
public class AssignmentStatement extends StatementWithOutputLine implements
    OutputWriter, SLPTReduction {
	// ~ Instance fields --------------------------------------------------------

	// data members

	/*
	 * An LvalExpression which is the LHS of the assignment.
	 */
	private LvalExpression lhs;

	/*
	 * A list of alternative forms for the RHS of this assignment.
	 * 
	 * Index 0 is the expression which is actually output. The remaining are used
	 * for expression combining and miscellaneous functions.
	 */
	private Expression[] rhs;

	/*
	 * The line number of this assignment statement in the output circuit. This
	 * number may vary form one transformation to another.
	 */
	private int outputLine = -1;

	// ~ Constructors -----------------------------------------------------------

	/**
	 * Constructs a new AssignmentStatement from a given lhs and rhs.
	 * 
	 * @param lhs
	 *          An LvalExpression which is the LHS of the assignment.
	 * @param rhs
	 *          An Expression which is the RHS of the assignment expression.
	 */
	public AssignmentStatement(LvalExpression lhs, Expression rhs) {
		this(lhs, new Expression[] { rhs });
	}

	public AssignmentStatement(LvalExpression lhs, Expression[] rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
		this.lhs.setAssigningStatement(this);
	}

	// ~ Methods ----------------------------------------------------------------

	public void dedicateAssignment() {
		// Type check this assignment.
		Type rhsType;
		if (typeCheck) {
			rhsType = new AnyType(); // Make sure that this assignment has the right
															 // size
		} else {
			rhsType = lhs.getType(); // Decrease the size if the assignment is
															 // smaller, otherwise don't change it
		}

		// We can probably do better, by examining the rhs
		for (Expression oe : rhs) {
			Type oeType = oe.getType();
			rhsType = TypeHeirarchy.looseIntersect(rhsType, oeType);
		}

		/*
		 * if (!TypeHeirarchy.isSubType(rhsType, lhs.getDeclaredType())) {
		 * typeError(
		 * "Type error, could not perform assignment "+this+", cannot assign "
		 * +rhsType+" to " + lhs.getName()+" of type "+lhs.getDeclaredType()); }
		 */

		// Pointer assignment type check
		{
			Type a = lhs.getDeclaredType();
			Type b = rhsType;
			if (a instanceof PointerType && b instanceof PointerType) {
				Type aPtdType = ((PointerType) a).getPointedToType();
				Type bPtdType = ((PointerType) b).getPointedToType();
				if (!(aPtdType instanceof AnyType)) { // AnyType => Allow any type of
																							// pointer (only internal code can
																							// use this type)
					if (TypeHeirarchy.equals(aPtdType, bPtdType)) {
						// Pointer assignment valid.
					} else {
						Type lhsTypePtd = aPtdType;
						Type rhsTypePtd = bPtdType;
						typeError("Type error, could not perform assignment " + this
						    + ", cannot assign pointer-to-" + rhsTypePtd + " to "
						    + lhs.getName() + " which is a pointer-to-" + lhsTypePtd + ".");
					}
				}
			}
		}

		// Ok! We fit into the bounds of the LHS, so replace its type.
		lhs.setType(rhsType);
	}

	private void typeError(String typeError) {
		if (typeCheckWarns) {
			System.out.println("Type Error set to WARNING mode: " + typeError);
		} else {
			throw new ClassCastException(typeError);
		}
	}

	/**
	 * Transforms this multibit AssignmentStatement into singlebit Statements and
	 * returns a BlockStatement containing the result.
	 * 
	 * @param obj
	 *          not used (null).
	 * @return BlockStatement containing singlebit Statements of this
	 *         AssignmentStatement.
	 */
	public Statement toSLPTCircuit(Object obj) {
		return ((SLPTReduction) rhs[0]).toSLPTCircuit(this);
	}

	/**
	 * Returns a string representation of this AssignmentStatement.
	 * 
	 * @return a string representation of this AssignmentStatement.
	 */
	public String toString() {
		return lhs.toString() + '=' + rhs[0].toString() + "\n";
	}

	/**
	 * Returns this AssignmentStatement's rhs.
	 * 
	 * @return this AssignmentStatement's rhs.
	 */
	public Expression getRHS() {
		return rhs[0];
	}

	public Expression[] getAllRHS() {
		return rhs;
	}

	public void addAlternativeRHS(Expression alt) {
		Expression[] newRHS = new Expression[rhs.length + 1];
		System.arraycopy(rhs, 0, newRHS, 0, rhs.length);
		newRHS[rhs.length] = alt;
		rhs = newRHS;
	}

	/**
	 * Returns true if this assingment statement can be inlined as an LVAL, a
	 * CONST, or a MUX. ( all of these have comparatively small memory use
	 * representations ) public boolean hasSmallInlinedForm(){
	 * for(InliningConstraints ic : new InliningConstraints[]{
	 * InliningConstraints.INLINE_LVAL_CONST, InliningConstraints.INLINE_MUX, }){
	 * if (canInline[ic.ordinal()] == null){ //Try it! inline(null, ic); } if
	 * (canInline[ic.ordinal()]){ return true; } } return false; }
	 */

	public void clearRHS() {
		rhs = new Expression[0];
	}

	/**
	 * Removes all rhs data except for consts, lvals, and muxes.
	 */
	public void clearMemoryIntensiveRHS() {
		ArrayList<Expression> newRHS = new ArrayList();
		for (Expression q : rhs) {
			if (q instanceof ConstExpression || q instanceof LvalExpression
			    || q instanceof MuxPolynomialExpression || q instanceof Pointer) {
				newRHS.add(q);
			}
		}
		Expression[] newRHS_ = new Expression[newRHS.size()];
		newRHS.toArray(newRHS_);
		rhs = newRHS_;
	}

	/**
	 * Returns this AssignmentStatement's lhs.
	 * 
	 * @return this AssignmentStatement's lhs.
	 */
	public LvalExpression getLHS() {
		return lhs;
	}

	/**
	 * Tries all alternative representations of the rhs for inlining. public
	 * Expression inline(Object obj, InliningConstraints mode) { //Optimization:
	 * Remember whether we've failed to inline this AS under these constraints,
	 * and don't try again if so if (canInline[mode.ordinal()] != null){ if
	 * (!canInline[mode.ordinal()]){ return null; } }
	 * 
	 * for(OperationExpression oe : rhs){ Expression inlineRhs =
	 * ((Inlineable)oe).inline(null, mode); if (inlineRhs != null){
	 * canInline[mode.ordinal()] = true; return inlineRhs; } }
	 * canInline[mode.ordinal()] = false; return null; }
	 */

	/**
	 * Emit this assignment statement, in optimized form
	 */
	public void toAssignmentStatements(StatementBuffer assignments) {
		// Update the references of the rhs
		for (int i = 0; i < rhs.length; i++) {
			rhs[i] = rhs[i].changeReference(Function.getVars());
		}

		toAssignmentStatements_NoChangeRef(assignments);
	}

	/**
	 * Emit his assignment statement, but do not attempt to re-resolve the rhs's
	 * references.
	 * 
	 * (This is needed by the if statement expansion, which manually evaluates all
	 * rhs references.)
	 */
	public void toAssignmentStatements_NoChangeRef(StatementBuffer assignments) {
		if (rhs.length == 0) {
			throw new RuntimeException("Zero length RHS");
		}
		BitString bs_of_lhs = null;

		// Inline the RHS.
		for (int i = 0; i < rhs.length; i++) {
			// Loop 4x on each.
			for (int j = 0; j < 4; j++) {
				if (rhs[i] instanceof BitString) {
					// Convert the bitstring to a polynomialExpression, but remember that
					// this LHS can now be represented
					// as a bit string.
					BitString asBS = (BitString) rhs[i];
					if (bs_of_lhs == null || asBS.size() < bs_of_lhs.size()) {
						// More than one RHS may offer a bit string. Keep the shortest one.
						bs_of_lhs = asBS;
					}
					rhs[i] = PolynomialExpression.toPolynomialExpression(rhs[i]);
				} else {
					Expression neu = ((Inlineable) rhs[i]).inline(null, assignments);
					if (neu == null) {
						throw new RuntimeException("Failure in inlining routine: " + rhs[i]);
					}
					/*
					 * if (neu instanceof LvalExpression){ //It is unsafe to steal the
					 * bitString here, because bitStrings are not allowed to tunnel
					 * through assignments BitString asBS = ((LvalExpression)
					 * neu).getBitString(); if (asBS != null){ if (bs_of_lhs == null ||
					 * asBS.size() < bs_of_lhs.size()){ //More than one RHS may offer a
					 * bit string. Keep the shortest one. bs_of_lhs = asBS; } } }
					 */
					rhs[i] = neu;
				}
			}
		}

		// Get the next available number in the program.
		outputLine = Program.getLineNumber();
		if (outputLine == 6844) {
			// System.out.println(outputLine);
		}

		// After calling this, RHS CANNOT be changed.
		updateReferences();

		// get the last reference existing
		Statement oldAssignment = null;
		Map oldLhsScope = null;
		Map newLhsScope = null;

		if (Function.getVar(lhs) != null) { // CompiledStatement does not use the
																				// VariableLUT system.

			// an entry in the VariableLUT
			oldLhsScope = Function.getVars().getVarScope(lhs.getName());
			oldAssignment = Function.getVar(lhs).getAssigningStatement();

			// this assignment updates lhs, which invalidates its value got from the
			// the previous assignment.
			// overwrite this reference with this assignment
			if (oldAssignment != this) {
				Function.addVar(lhs); // .getName(), lhs.getType(), lhs.isOutput());

				// get the new ref to lhs
				lhs = Function.getVar(lhs);
				// this assignment updates lhs, which invalidates its value got from the
				// the previous assignment.
				// overwrite this reference with this assignment
				lhs.setAssigningStatement(this);
			}

			newLhsScope = Function.getVars().getVarScope(lhs.getName());
		}
		// Assign the bitstring, if we have one here (couldn't do it before the LHS
		// was resolved, which must come after inlining the RHS)
		if (lhs.getBitString() != null) {
			throw new RuntimeException("Assertion error");
		}
		if (bs_of_lhs != null) {
			lhs.setBitString((BitString) bs_of_lhs);
		}

		// Update types / perform type checking
		dedicateAssignment();

		// Emit this assignment, and register all references
		assignments.add(this);

		if (Optimizer.isFirstPass()) {
			// Deadness of temporary variables which this assignment depends on
			for (AssignmentStatement as : assignments
			    .getTemporaryVariableAssignmentsFor(lhs.getName())) {
				// If the temporary variable was never used, attempt to prevent it from
				// getting to disk.
				if (!as.getLHS().isReferenced()) {
					assignments.callbackAssignment(as);
				}
				assignments.cleanupAssignmentData(as);
			}

			// Deadness of the old assignment to lhs. We only handle overwriting of
			// assignment statements which are in the same
			// scope as this one.
			if (oldAssignment != this
			    && (oldAssignment instanceof AssignmentStatement)
			    && newLhsScope == oldLhsScope) {
				AssignmentStatement as = (AssignmentStatement) oldAssignment;
				// If the old assignment was never used, attempt to prevent it from
				// getting to disk.
				if (!as.getLHS().isReferenced()) {
					assignments.callbackAssignment(as);
				}
				assignments.cleanupAssignmentData(as);
			}
		}

		// Now detect if this assignment is a duplicate of a prior assignment
		if (doRedundantVarAnalysisForAllVariables) {
			/*
			 * 
			 * TODO: get redundant variable analysis back on a local scale. We can do
			 * it!
			 * 
			 * //Determine whether replacing the RHS with a pointer to an lval will
			 * actually be a simplification: boolean attemptMemo =
			 * !isIdentityOrConstant(rhs[0]);
			 * 
			 * if (attemptMemo){ LvalExpression existing =
			 * Optimizer.getLvalFor(rhs[0]); if (existing != null) { //If the existing
			 * reference is not an output, and the new reference is, replace the
			 * existing one if (!existing.isOutput() && lhs.isOutput()){
			 * Optimizer.addAssignment(rhs[0], lhs); } else{ //Otherwise replace our
			 * RHS with a pointer to the existing lvalexpression.
			 * removeAllReferences(); rhs[0] = new UnaryOpExpression(new
			 * UnaryPlusOperator(), existing); //This creates a new reference to the
			 * existing lvalexpression existing.addReference(this); } } else { //Add
			 * this assignment to the memo Optimizer.addAssignment(rhs[0], lhs); } }
			 */
		}
	}

	/**
	 * Add any necessary references imposed by the RHS of this assignment
	 * statement
	 */
	private void updateReferences() {
		for (Expression oe : rhs) {
			if (oe instanceof LvalExpression) {
				((LvalExpression) oe).addReference();
			}
			for (LvalExpression lv : oe.getLvalExpressionInputs()) {
				lv.addReference();
			}
		}
	}

	/**
	 * Remove all references held by the RHS of this assignment statement
	 */
	public void removeAllReferences() {
		for (Expression oe : rhs) {
			if (oe instanceof LvalExpression) {
				((LvalExpression) oe).removeReference();
			}
			for (LvalExpression lv : oe.getLvalExpressionInputs()) {
				lv.removeReference();
			}
		}
	}

	/**
	 * Prints this AssignmentStatement into the circuit.
	 * 
	 * @param circuit
	 *          the circuit output file.
	 */
	public void toCircuit(Object obj, PrintWriter circuit) {
		circuit.print(outputLine + " " + ((lhs.isOutput()) ? "output " : ""));
		circuit.print("gate ");
		Expression toOutput = rhs[0];
		if (!(toOutput instanceof OperationExpression)) {
			toOutput = new UnaryOpExpression(new UnaryPlusOperator(), toOutput);
		}
		if (((OperationExpression) toOutput).getOperator() instanceof OutputsToPolynomial) {
			circuit.print("poly inputs [ ");
			PolynomialExpression pe = PolynomialExpression
			    .toPolynomialExpression(rhs[0]);
			pe.toCircuit(null, circuit);
		} else {
			circuit.print(((OperationExpression) toOutput).getOperator().toString()
			    + " inputs [ ");
			((OutputWriter) toOutput).toCircuit(null, circuit);
		}
		circuit.print(" ]\t//");
		circuit.print(lhs.getName() + " " + lhs.getType());// ((lhs.isOutput()) ?
																											 // (lhs.getName()) :
																											 // "anonymous"));
		circuit.println();
	}

	/**
	 * Returns an int that represents the line number of this assignmnet statement
	 * in the output circuit.
	 * 
	 * @return an int that represents the line number of this assignmnet statement
	 *         in the output circuit.
	 */
	public int getOutputLine() {
		return outputLine;
	}

	/**
	 * Sets the output line of this assignment statement.
	 * 
	 * @param line
	 *          the line number in the output.
	 */
	public void setOutputLine(int line) {
		outputLine = line;
	}

	/**
	 * Returns a replica of this statement.
	 * 
	 * @return a replica of this statement.
	 */
	public Statement duplicate() {
		Expression[] newRHS = new Expression[rhs.length];
		for (int i = 0; i < rhs.length; i++) {
			newRHS[i] = rhs[i].duplicate();
		}
		AssignmentStatement as = new AssignmentStatement(
		    (LvalExpression) lhs.duplicate(), newRHS);
		as.setOutputLine(getOutputLine());
		return as;
	}

	/**
	 * This can sometimes get in the way of expression combination
	 */
	public static boolean doRedundantVarAnalysisForAllVariables = false;

	/**
	 * When disabled, IF statements aren't combined,
	 */
	public static boolean combineExpressions = true;

	/**
	 * When enabled, a suitable constant is multiplied by each polynomial so that
	 * there are no fractions inside it. (The polynomial is then written (1/c) *
	 * (scaled poly))
	 */
	public static boolean removeFractions;

	/**
	 * Enable / Disable type checking.
	 */
	public static boolean typeCheck = true;

	/**
	 * By default, type errors are lethal.
	 * 
	 * This reduces them to warnings.
	 */
	public static boolean typeCheckWarns = false;

	/**
	 * When turned off, optimizations cannot perform inlinings which would
	 * increase constraint size I.e. when this is turned on, the expected number
	 * of constraints goes down.
	 */
	public static boolean allowBigConstraints = true;

	/**
	 * Returns: - Null if a is not an lvalExpression - Otherwise, let b be the
	 * statement which assigns a value to a - Returns null if b is not an
	 * assignment statement - Otherwise, returns b
	 */
	public static AssignmentStatement getAssignment(Expression a) {
		LvalExpression lvalA = LvalExpression.toLvalExpression(a);
		if (lvalA != null) {
			Statement as = lvalA.getAssigningStatement();
			if (as instanceof AssignmentStatement) {
				return (AssignmentStatement) as;
			}
		}
		return null;
	}
}
