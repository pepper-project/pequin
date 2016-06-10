// PlusOperator.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler.Operators;

import SFE.Compiler.BinaryOpExpression;
import SFE.Compiler.BooleanType;
import SFE.Compiler.Expression;
import SFE.Compiler.FloatConstant;
import SFE.Compiler.FloatType;
import SFE.Compiler.IntConstant;
import SFE.Compiler.IntType;
import SFE.Compiler.Optimizer;
import SFE.Compiler.OutputsToPolynomial;
import SFE.Compiler.Pointer;
import SFE.Compiler.PolynomialExpression;
import SFE.Compiler.PolynomialExpression.PolynomialTerm;
import SFE.Compiler.RestrictedFloatType;
import SFE.Compiler.RestrictedSignedIntType;
import SFE.Compiler.RestrictedUnsignedIntType;
import SFE.Compiler.SLPTReduction;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;
import SFE.Compiler.TypeHeirarchy;

/**
 * A class for representing binary + operator expressions that can be defined in
 * the program.
 */
public class PlusOperator extends ScalarOperator implements SLPTReduction,
    OutputsToPolynomial {
	// ~ Methods ----------------------------------------------------------------

	/**
	 * Returns a string representation of the object.
	 */
	public String toString() {
		return "+";
	}

	/**
	 * Returns 2 as the arity of this PlusOperator. Arity is 1 for unary ops; 2
	 * for binary ops; 3 for ternary ops; 0 for constants
	 * 
	 * @return 2 as the arity of this PlusOperator.
	 */
	public int arity() {
		return 2;
	}

	/**
	 * Inlines the expression containing a plus operator, obj.
	 */
	public Expression inlineOp(StatementBuffer assignments, Expression... args) {
		Expression left = args[0];
		Expression right = args[1];

		PolynomialExpression pleft = PolynomialExpression
		    .toPolynomialExpression(left);
		PolynomialExpression pright = PolynomialExpression
		    .toPolynomialExpression(right);

		if (pleft != null && pright != null) {
			boolean allowedSum = false;
			switch (Optimizer.optimizationTarget) {
			case GINGER:
				allowedSum = pleft.getDegree() <= 2 && pright.getDegree() <= 2;
				break;
			case ZAATAR:
				allowedSum = (pleft.getDegree() <= 2 && pright.getDegree() <= 1)
				    || (pleft.getDegree() <= 1 && pright.getDegree() <= 2);
				break;
			}
			if (allowedSum) {
				// Add whichever has the smaller number of terms to the other.
				PolynomialExpression sum;
				if (pleft.getTerms().size() > pright.getTerms().size()) {
					sum = pleft;
					for (PolynomialTerm pt : pright.getTerms()) {
						sum.addMultiplesOfTerm(IntConstant.ONE, pt);
					}
				} else {
					sum = pright;
					for (PolynomialTerm pt : pleft.getTerms()) {
						sum.addMultiplesOfTerm(IntConstant.ONE, pt);
					}
				}

				return sum;
			}
		}

		return null;
	}

	/*
	 * This handles resolve a pointer like *(a+i) (or equivalently a[i])
	 */
	public Expression resolve(Expression... args) {
		Expression left = args[0];
		Expression right = args[1];

		FloatConstant lc = FloatConstant.toFloatConstant(left);
		FloatConstant rc = FloatConstant.toFloatConstant(right);
		if (lc != null && rc != null) {
			return lc.add(rc);
		}
		if (lc != null && lc.isZero()) {
			return right;
		}
		if (rc != null && rc.isZero()) {
			return left;
		}

		IntConstant li = IntConstant.toIntConstant(left);
		IntConstant ri = IntConstant.toIntConstant(right);
		if (li != null) {
			// Index into pointer
			Pointer rp = Pointer.toPointerConstant(right);
			if (rp != null) {
				return rp.increment(li);
			}
		}
		if (ri != null) {
			// Index into pointer
			Pointer lp = Pointer.toPointerConstant(left);
			if (lp != null) {
				return lp.increment(ri);
			}
		}

		return null;
	}

	public Type getType(Object obj) {
		BinaryOpExpression expr = (BinaryOpExpression) obj;

		Type a = expr.getLeft().getType();
		Type b = expr.getRight().getType();
		Type union = TypeHeirarchy.looseUnion(a, b);
		return sumTypes(2, union);

		/*
		 * List<Type> types = new ArrayList(2); types.add(expr.getLeft().getType());
		 * types.add(expr.getRight().getType());
		 * 
		 * return sumTypes(types);
		 */
	}

	public static double log2(double a) {
		return Math.log(a) / logE2;
	}

	private static double logE2 = Math.log(2);

	/**
	 * Produces the type that is produced by adding together m objects of type A
	 */
	public static Type sumTypes(int m, Type A) {
		if (m == 0) {
			return new BooleanType();
		}
		if (m == 1) {
			return A;
		}
		if (A instanceof RestrictedUnsignedIntType) {
			RestrictedUnsignedIntType ruit = (RestrictedUnsignedIntType) A;
			return new RestrictedUnsignedIntType(ruit.getInterval().addNtimes(m));
		}
		if (A instanceof RestrictedSignedIntType) {
			RestrictedSignedIntType rsit = (RestrictedSignedIntType) A;
			return new RestrictedSignedIntType(rsit.getInterval().addNtimes(m));
		}
		if (A instanceof RestrictedFloatType) {
			int maxNa = ((RestrictedFloatType) A).getNa();
			int maxNb = ((RestrictedFloatType) A).getNb();
			return new RestrictedFloatType((int) Math.ceil(log2(m) + maxNa + maxNb),
			    maxNb);
		}

		// Infinite types
		if (A instanceof FloatType || A instanceof IntType) {
			return A;
		}
		throw new ClassCastException(
		    "Type error: Cannot define addition of terms of type " + A);
	}

	/*
	 * public static Type sumTypes(List<Type> termTypes) { if
	 * (termTypes.isEmpty()){ return new BooleanType(); //0 } if (termTypes.size()
	 * == 1){ return termTypes.get(0); }
	 * 
	 * int m = termTypes.size();
	 * 
	 * boolean isAllUnsignedInts = true; int maxNa = 1; for(Type q : termTypes){
	 * if (q instanceof RestrictedUnsignedIntType){ RestrictedUnsignedIntType q2 =
	 * (RestrictedUnsignedIntType)q; maxNa = Math.max(maxNa, q2.getLength()); }
	 * else { isAllUnsignedInts = false; break; } } if (isAllUnsignedInts){ return
	 * new RestrictedUnsignedIntType((int)Math.ceil(log2(m) + maxNa)); }
	 * 
	 * boolean isAllInts = true; maxNa = 2; for(Type q : termTypes){ if (q
	 * instanceof RestrictedSignedIntType){ RestrictedSignedIntType q2 =
	 * (RestrictedSignedIntType)q; maxNa = Math.max(maxNa, q2.getLength()); } else
	 * if (q instanceof RestrictedUnsignedIntType){ RestrictedUnsignedIntType q2 =
	 * (RestrictedUnsignedIntType)q; maxNa = Math.max(maxNa, q2.getLength() + 1);
	 * } else { isAllInts = false; break; } } if (isAllInts){ return new
	 * RestrictedSignedIntType((int)Math.ceil(log2(m) + maxNa)); }
	 * 
	 * boolean isAllFloats = true; maxNa = 1; int maxNb = 1; for(Type q :
	 * termTypes){ if (q instanceof RestrictedSignedIntType){
	 * RestrictedSignedIntType q2 = (RestrictedSignedIntType)q; maxNa =
	 * Math.max(maxNa, q2.getLength()); // value >= -2^{q2.length} + 1 } else if
	 * (q instanceof RestrictedUnsignedIntType){ RestrictedUnsignedIntType q2 =
	 * (RestrictedUnsignedIntType)q; maxNa = Math.max(maxNa, q2.getLength()); }
	 * else if (q instanceof RestrictedFloatType){ RestrictedFloatType q2 =
	 * (RestrictedFloatType)q; maxNa = Math.max(maxNa, q2.getNa()); maxNb =
	 * Math.max(maxNb, q2.getNb()); } else { isAllFloats = false; break; } } if
	 * (isAllFloats){ return new RestrictedFloatType((int)Math.ceil(log2(m) +
	 * maxNa + maxNb), maxNb); }
	 * 
	 * throw new
	 * ClassCastException("Type error: Cannot define addition of terms "+
	 * termTypes); }
	 */

	/**
	 * Returns an int that represents the priority of the operator
	 * 
	 * @return an int that represents the priority of the operator
	 */
	public int priority() {
		return 2;
	}
}
