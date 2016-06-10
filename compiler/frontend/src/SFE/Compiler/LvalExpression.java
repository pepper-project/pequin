// LvalExpression.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Vector;

import SFE.Compiler.Operators.UnaryPlusOperator;

/**
 * The LvalExpression class represents an Expression that can appear as LHS in
 * the program.
 */
public class LvalExpression extends Expression implements OutputWriter,
    Inlineable {

//	private static HashMap<String, LvalExpression> notCached = new HashMap<String, LvalExpression>();

	// ~ Instance fields
	// --------------------------------------------------------

	// data members

	/*
	 * Holds the lvalue of this Expression
	 */
	private Lvalue lvalue;
	private BitString bitString;

	private StatementWithOutputLine assigningStatement = null;
	private int referenceCount = 0;
	private int referenceCountUB = -1;
	private int killPoint = Integer.MAX_VALUE;

	// private List<AssignmentStatement> references = null;

	// ~ Constructors
	// -----------------------------------------------------------
	/**
	 * LvalExpression constractor
	 * 
	 * @param lvalue
	 */
	public LvalExpression(Lvalue lvalue) {
		this.lvalue = lvalue;
	}

	// ~ Methods
	// ----------------------------------------------------------------

	/**
	 * Returns a string representation of the object.
	 * 
	 * @return a string representation of the object.
	 */
	public String toString() {
		if (assigningStatement == null) {
			return "broken-ref:" + getName();
		}
		return Integer.toString(getOutputLine());
	}

	public int hashCode() {
		return getOutputLine();
	}

	public int getOutputLine() {
		if (assigningStatement == null) {
			throw new RuntimeException();
		}
		if (assigningStatement instanceof InputStatement) {
			return ((InputStatement) assigningStatement).getOutputLine();
		}
		return assigningStatement.getOutputLine();
	}

	/**
	 * Returns true if some output assignment (thus far) has this lvalexpression
	 * as a dependency.
	 */
	public boolean isReferenced() {
		return referenceCount > 0;
	}

	public int getReferenceCount() {
		return referenceCount;
	}

	public int getReferenceCountUB() {
		return this.referenceCountUB;
	}

	/*
	 * public List<AssignmentStatement> getReferences() { return null;
	 * //references; }
	 */

	/**
	 * Add a reference to this lval expression
	 * 
	 * @param as
	 */
	public void addReference() {
		referenceCount++;
	}

	/**
	 * Remove a reference from this lval expression
	 */
	public void removeReference() {
		referenceCount--;
	}

	/**
	 * Returns this Lavlue.
	 * 
	 * @return this Lavlue.
	 */
	public Lvalue getLvalue() {
		return lvalue;
	}

	/**
	 * Returns the name of this LvalExpression's lvalue.
	 * 
	 * @return a string representing this LvalExpression's lvalue.
	 */
	public String getName() {
		return lvalue.getName();
	}

	/**
	 * Returns the Type of this LvalExpression's lvalue.
	 * 
	 * @return the Type of this LvalExpression's lvalue.
	 */
	public Type getType() {
		return lvalue.getType();
	}

	/**
	 * Returns the declared type of this LvalExpression's lvalue
	 */
	public Type getDeclaredType() {
		return ((VarLvalue) lvalue).getDeclaredType();
	}

	/**
	 * Sets the Type of this LvalExpression's lvalue.
	 */
	public void setType(Type T) {
		lvalue.setType(T);
	}

	/**
	 * Returns true if the this expression is a part out the circuit's output.
	 * 
	 * @return true if the this expression is a part out the circuit's output.
	 */
	public boolean isOutput() {
		return lvalue.isOutput();
	}

	/**
	 * Set the reference to this expressionn assigning statement, Which can be
	 * either AssignmentStatement or InputStatement.
	 * 
	 * @param as
	 *          the assigning statement.
	 */
	public void setAssigningStatement(StatementWithOutputLine as) {
		if (this.assigningStatement == null) {
			this.assigningStatement = as;
		}
	}

	public void setAssigningStatement(StatementWithOutputLine as, boolean force) {
		if (this.assigningStatement == null || force) {
			this.assigningStatement = as;
		}
	}

	public void clearAssigningStatement() {
		assigningStatement = null;
	}

	/**
	 * Returns the assigning statement of this lvalexpression.
	 * 
	 * @return the assigning statement of this lvalexpression.
	 */
	public StatementWithOutputLine getAssigningStatement() {
		if (assigningStatement != null) {
			if (assigningStatement.getOutputLine() < 0) {
				return null; // Not valid until the assignment statement is
				// actually
				// output.
			}
		}
		return assigningStatement;
	}

	public Expression evaluateExpression(String goal, String tempName,
	    BlockStatement result) {
		if (lvalue.hasAddress()) {
			int address = getAddress();
			if (!CacheManager.isCached(address)) {
				// no need to create a temporary, we are already an Lval
				Statement ramget = new RamGetEnhancedStatement(this,
				    IntConstant.valueOf(address));
				ramget = ramget.toSLPTCircuit(null);
				result.addStatement(ramget);

				CacheManager.addCache(address);
			}
		}

		// otherwise, this should be an intermediate variable.
		// no corresponding RAM location.
		return this;
	}

	/**
	 * Prints this LvalExpression into the circuit.
	 * 
	 * @param circuit
	 *          the circuit output file.
	 */
	public void toCircuit(Object obj, PrintWriter circuit) {
		if (assigningStatement instanceof InputStatement) {
			circuit.print(((InputStatement) assigningStatement).getOutputLine());
		} else {
			circuit.print(assigningStatement.getOutputLine());
		}
	}

	public Expression inline(Object obj, StatementBuffer assignments) {
		if (true) {
			return new UnaryOpExpression(new UnaryPlusOperator(), this).inline(null,
			    assignments);
		}

		// We couldn't inline the referenced assignment, but can we inline this
		// object?
		if (assigningStatement instanceof AssignmentStatement) {
			// Try to substitute the assigning statement's rhs in, if available
			AssignmentStatement as = (AssignmentStatement) assigningStatement;
			// Inline the referenced expression (recursively inlines)
			Expression inlineRhs;

			for (Expression toRet : ((AssignmentStatement) as).getAllRHS()) {
				ConstExpression ce = ConstExpression.toConstExpression(toRet);
				if (ce != null) {
					return ce;
				}
				LvalExpression le = LvalExpression.toLvalExpression(toRet);
				if (le != null) {
					return le;
				}
			}

			if (referenceCountUB == 1 && !this.isOutput() && // don't inline
			    // through
			    // the final
			    // output
			    // lines,
			    // because it
			    // won't remove
			    // a
			    // variable
			    AssignmentStatement.combineExpressions) {
				// If an RHS is available, return it and trash the expression
				/*
				 * Unsafe, references may already be established... for(Expression toRet
				 * : ((AssignmentStatement) as).getAllRHS()) {
				 * assignments.callbackAssignment(as); return toRet; }
				 */
			}
		}
		return this;
	}

	public LvalExpression changeReference(VariableLUT unique) {
		// Fix references that weren't covered in toSLPT.
		String name = getName();
		// Return the unique version of the variable with this name.
		LvalExpression toRet = unique.getVar(name);
		if (toRet == null) {
			throw new RuntimeException();
		}
		return toRet;
	}

	public Vector getUnrefLvalInputs() {
		Vector toRet = new Vector();
		if (!isReferenced()) {
			toRet.add(this);
		}
		return toRet;
	}

	/**
	 * sets this LvalExpression as a pin that is not an output of this circuit.
	 */
	public void notOutput() {
		lvalue.notOutput();
	}

	/**
	 * Indicate that this lval is referenced at most x times.
	 * 
	 * @param refCount
	 */
	public void setUBRefCount(int refCount) {
		referenceCountUB = refCount;
	}

	/**
	 * Indicate that this lval is not referenced after statement killPoint
	 */
	public void setKillPoint(int killPoint) {
		if (killPoint < 0) {
			killPoint = Integer.MAX_VALUE;
		}
		this.killPoint = killPoint;
	}

	public int getKillPoint() {
		return killPoint;
	}

	public static LvalExpression toLvalExpression(Expression c) {
		if (c instanceof LvalExpression) {
			return (LvalExpression) c;
		}
		if (c instanceof UnaryOpExpression) {
			UnaryOpExpression uo = ((UnaryOpExpression) c);
			return toLvalExpression(uo.getOperator().resolve(uo.getMiddle()));
		}
		if (c instanceof BinaryOpExpression) {
			BinaryOpExpression bo = ((BinaryOpExpression) c);
			return toLvalExpression(bo.getOperator().resolve(bo.getLeft(),
			    bo.getRight()));
		}
		return null;
	}

	public void setBitString(BitString other) {
		this.bitString = other;
	}

	public BitString getBitString() {
		return bitString;
	}

	public BitString getInlinedBitString() {
		BitString now = getBitString();
		AssignmentStatement as = AssignmentStatement.getAssignment(this);
		if (as != null) {
			for (Expression q : as.getAllRHS()) {
				LvalExpression asLval = LvalExpression.toLvalExpression(q);
				if (asLval != null) {
					BitString cand = asLval.getInlinedBitString();
					if (cand != null) {
						// Multiple bitStrings may exist for this lval, keep the
						// shortest
						if (now == null || now.size() > cand.size()) {
							now = cand;
						}
					}
				}
			}
		}
		return now;
	}

	public List<LvalExpression> getDerivedLvalues() {
		List<LvalExpression> derived = new ArrayList();
		for (Object q : lvalue.getDerivedLvalues()) {
			derived.add(Function.getVars().getVar(((Lvalue) q).getName()));
		}
		return derived;
	}

	/**
	 * Will not use the Lval table if the new name is equal to the current name.
	 */
	public LvalExpression fieldEltAt(int i) {
		String bitName = lvalue.getType().fieldEltAt(lvalue.getName(), i);
		if (bitName.equals(lvalue.getName())) {
			return this;
		}

		LvalExpression var = Function.getVars().getVar(bitName);
		if (var == null) {
			throw new RuntimeException("Couldn't get bit " + i + " of " + this);
		}
		return var;
	}

	public LvalExpression lvalFieldEltAt(int i) {
		return (LvalExpression) fieldEltAt(i);
	}

	public int size() {
		return lvalue.getType().size();
	}

	public Collection<LvalExpression> getLvalExpressionInputs() {
		return Collections.<LvalExpression> emptyList();
	}
	
	public boolean hasAddress() {
		return lvalue.hasAddress();
  }
	
	public int getAddress() {
		return lvalue.getAddress();
	}
	
	public void allocateStackAddress() {
		lvalue.allocateStackAddress();
		CacheManager.setMemoryMapping(this);
	}
	
	public void allocateHeapAddress() {
		lvalue.allocateHeapAddress();
		CacheManager.setMemoryMapping(this);
	}
}