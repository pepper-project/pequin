package SFE.Compiler;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.AllPermission;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

import SFE.Compiler.Operators.MinusOperator;
import SFE.Compiler.Operators.Operator;
import SFE.Compiler.Operators.PlusOperator;
import SFE.Compiler.Operators.TimesOperator;
import SFE.Compiler.Operators.UnaryMinusOperator;
import SFE.Compiler.Operators.UnaryPlusOperator;

/**
 * Polynomial from primitive type monomers to primitive type outputs.
 */
public class PolynomialExpression extends OperationExpression implements
    Inlineable, OutputsToPolynomial, Comparable<PolynomialExpression> {
	/**
	 * The operator standing for a polynomial evaluation (dummy implementation)
	 */
	public static class PolynomialOperator extends Operator implements
	    OutputsToPolynomial {
		public int arity() {
			return 0;
		}

		public int priority() {
			return 0;
		}

		public Type getType(Object obj) {
			throw new RuntimeException();
		}

		public Expression inlineOp(StatementBuffer sb, Expression... args) {
			throw new RuntimeException("Not implemented");
		}

		public Expression resolve(Expression... args) {
			return null;
		}

		public Expression fieldEltAt(Expression expression, int i) {
			throw new RuntimeException("Not yet implemented");
		}
	}

	public static class PolynomialMonomer implements
	    Comparable<PolynomialMonomer> {
		public int compareTo(PolynomialMonomer arg0) {
			return lvalToNum(var) - lvalToNum(arg0.var);
		}

		public boolean equals(Object o) {
			if (o == null) {
				return false;
			}
			PolynomialMonomer other = (PolynomialMonomer) o;
			return compareTo(other) == 0;
		}

		public String toString() {
			return var.toString();
		}

		public LvalExpression var;

		public PolynomialMonomer(LvalExpression var) {
			if (var.size() != 1) {
				throw new RuntimeException("Polynomial monomers must be primitive");
			}
			this.var = var;
		}
	}

	public static class PolynomialTerm implements Comparable<PolynomialTerm> {
		public FloatConstant constant;
		private List<PolynomialMonomer> monomerFactors;
		private List<PolynomialExpression> polyFactors;
		private int degree;

		public PolynomialTerm() {
			constant = FloatConstant.ONE;
			monomerFactors = new ArrayList(1);
			polyFactors = new ArrayList(1);
			degree = 0;
		}

		public PolynomialTerm duplicate() {
			PolynomialTerm dup = new PolynomialTerm();
			dup.constant = this.constant;
			dup.monomerFactors.addAll(this.monomerFactors);
			for (PolynomialExpression pe : polyFactors) {
				dup.polyFactors.add((PolynomialExpression) pe.duplicate());
			}
			dup.degree = degree;
			// Collections.sort(dup.monomerFactors);
			// Collections.sort(dup.polyFactors);
			// dup.simplifyProduct();
			return dup;
		}

		private static PolynomialTerm productTerm(PolynomialTerm ptL,
		    PolynomialTerm ptR) {
			PolynomialTerm product = new PolynomialTerm();
			product.constant = ptL.constant;
			product.constant = product.constant.multiply(ptR.constant);
			product.monomerFactors.addAll(ptL.monomerFactors);
			product.monomerFactors.addAll(ptR.monomerFactors);
			for (PolynomialExpression pe : ptL.polyFactors) {
				product.addFactor(pe);
			}
			for (PolynomialExpression pe : ptR.polyFactors) {
				product.addFactor(pe);
			}
			Collections.sort(product.monomerFactors);
			Collections.sort(product.polyFactors);
			product.simplifyProduct();
			return product;
		}

		public int getNumPolynomialFactors() {
			return polyFactors.size();
		}

		public int getNumMonomerFactors() {
			return monomerFactors.size();
		}

		public int getDegree() {
			return degree;
		}

		public PolynomialExpression getPolynomialFactor(int index) {
			return polyFactors.get(index);
		}

		public LvalExpression getMonomerFactor(int index) {
			return monomerFactors.get(index).var;
		}

		/*
		 * public void addFactors(Collection in){ for(Object o : in){ if (Object o
		 * instanceof PolynomialMonomer){
		 * 
		 * } monomerFactors.add(pm); } Collections.sort(polyFactors);
		 * Collections.sort(monomerFactors); simplifyProduct(); }
		 */

		public void addFactor(Expression e) {
			FloatConstant asFC = FloatConstant.toFloatConstant(e);
			if (asFC != null) {
				constant = constant.multiply(asFC);
				return;
			}
			LvalExpression asLval = LvalExpression.toLvalExpression(e);
			if (asLval != null) {
				addFactor(new PolynomialMonomer(asLval));
				return;
			}
			PolynomialExpression pe = PolynomialExpression.toPolynomialExpression(e);
			if (pe != null) {
				// If pe has only one term add the factors of that term as factors.
				if (pe.getTerms().size() == 1) {
					for (PolynomialTerm toAdd : pe.terms) {
						// if (toAdd.constant.isOne()){
						constant = constant.multiply(toAdd.constant);
						for (PolynomialMonomer pm : toAdd.monomerFactors) {
							monomerFactors.add(pm);
						}
						for (PolynomialExpression pe2 : toAdd.polyFactors) {
							addFactor(pe2);
						}
						Collections.sort(monomerFactors);
						Collections.sort(polyFactors);
						simplifyProduct();
						return;
						// }
					}
				}
				if (pe.getTerms().size() == 0) {
					constant = FloatConstant.ZERO;
					return;
				}
				polyFactors.add(pe);
				Collections.sort(polyFactors);
				simplifyProduct();
				return;
			}

			throw new RuntimeException("Unable to add factor of type " + e);
			/*
			 * if (e instanceof LvalExpression) { LvalExpression in =
			 * (LvalExpression)e; addFactor(new PolynomialMonomer(in)); } else if (e
			 * instanceof ConstExpression) { ConstExpression in = (ConstExpression)e;
			 * FloatConstant fc = FloatConstant.toFloatConstant(in); constant =
			 * constant.multiply(fc); } else if (e instanceof PolynomialExpression) {
			 * PolynomialExpression pe = (PolynomialExpression)e; //If pe has only one
			 * term add the factors of that term as factors. if (pe.getTerms().size()
			 * == 1) { for(PolynomialTerm toAdd : pe.terms) { //if
			 * (toAdd.constant.isOne()){ constant = constant.multiply(toAdd.constant);
			 * for(PolynomialMonomer pm : toAdd.monomerFactors) {
			 * monomerFactors.add(pm); } for(PolynomialExpression pe2 :
			 * toAdd.polyFactors) { addFactor(pe2); }
			 * Collections.sort(monomerFactors); Collections.sort(polyFactors);
			 * simplifyProduct(); return; //} } } if (pe.getTerms().size() == 0) {
			 * constant = FloatConstant.ZERO; return; } polyFactors.add(pe);
			 * Collections.sort(polyFactors); simplifyProduct(); } else { throw new
			 * RuntimeException("Unable to add factor of type "+e); }
			 */
		}

		public void addFactor(PolynomialMonomer in) {
			monomerFactors.add(in);
			Collections.sort(monomerFactors);
			simplifyProduct();
		}

		/*
		 * Finds opportunities for simplifying the product, by exploiting boolean
		 * and zero_or_pm_one values
		 */
		private void simplifyProduct() {

			PolynomialMonomer currentMatch = null;
			int currentMultiplicity = 0;
			for (ListIterator<PolynomialMonomer> pmI = monomerFactors.listIterator(); pmI
			    .hasNext();) {
				PolynomialMonomer pm = pmI.next();
				if (pm.equals(currentMatch)) {
					currentMultiplicity++;
					// Decide whether to add this copy depending on whether the monomer is
					// a boolean or a zero_or_pm_one value
					if (TypeHeirarchy.isSubType(pm.var.getLvalue().getType(),
					    new BooleanType())) {
						// a^2 = a, so don't add past the first one
						pmI.remove();
					}
					/*
					 * else if (pm.var.getLvalue().isZeroOrPMOne()){ //a^3 = a, so don't
					 * add past the second if (currentMultiplicity > 2){ pmI.remove(); } }
					 */
					// Could do more, but we'll leave them for now.
				} else {
					currentMatch = pm;
					currentMultiplicity = 1;
				}
			}

			degree = monomerFactors.size();
			for (PolynomialExpression q : polyFactors) {
				degree += q.getDegree();
			}
		}

		/**
		 * First compares by degree (high degree terms come first), if same degree,
		 * compare by number of monomerFactors (more comes first), if same number,
		 * lexicographical comparison if same monomerfactors, compare by number of
		 * polyFactors (more comes first), if same number, lexicographical compare.
		 * Otherwise equal.
		 */
		public int compareTo(PolynomialTerm o) {
			int compareDegree = o.getDegree() - getDegree();
			if (compareDegree == 0) {
				// Lexicographic comparison of monomial factors
				int compareNumMonomers = o.getNumMonomerFactors()
				    - getNumMonomerFactors();
				if (compareNumMonomers == 0) {
					for (int i = 0; i < monomerFactors.size(); i++) {
						int cmp = monomerFactors.get(i).compareTo(o.monomerFactors.get(i));
						if (cmp != 0) {
							return cmp;
						}
					}
					// Ok, same monomer factors.

					int compareNumPolys = o.getNumPolynomialFactors()
					    - getNumPolynomialFactors();
					if (compareNumPolys == 0) {
						for (int i = 0; i < polyFactors.size(); i++) {
							int cmp = polyFactors.get(i).compareTo(o.polyFactors.get(i));
							if (cmp != 0) {
								return cmp;
							}
						}
						return 0; // Same polynomial term.
					}
					return compareNumPolys;
				}
				return compareNumMonomers;
			}
			return compareDegree;
		}

		/**
		 * Computes the type of this term.
		 */
		public Type getType() {
			Type toRet = constant.getType();

			for (PolynomialMonomer pm : monomerFactors) {
				toRet = TimesOperator.multiplyTypes(toRet, pm.var.getType());
			}

			for (PolynomialExpression pe : polyFactors) {
				toRet = TimesOperator.multiplyTypes(toRet, pe.getType());
			}

			return toRet;
		}

		/**
		 * Computes the required bits to evaluate this term. public void
		 * updateRequiredBits(Object object, Lvalue termSize) { if (getDegree() ==
		 * 0){ constant.updateRequiredBits(null, termSize); return; }
		 * 
		 * //We have at least one nonconstant factor Lvalue factorSize = new
		 * VarLvalue(new Variable("operatorBits$factor", new FloatType()), false);
		 * 
		 * factorSize.setUnbounded(); constant.updateRequiredBits(null, factorSize);
		 * 
		 * int na = 0, nb = 0; if (factorSize.isZeroOrPMOne()){ //The size is
		 * unchanged } else { if (factorSize.isUnbounded()){ return; } if
		 * (Math.abs(constant.getNumerator().value()) > 1){ na +=
		 * factorSize.getRequiredNumeratorBits(); } if
		 * (Math.abs(constant.getDenominator().value()) > 1){ nb +=
		 * factorSize.getRequiredDenominatorBits(); } }
		 * 
		 * for(PolynomialMonomer pm : monomerFactors){ factorSize.setUnbounded();
		 * pm.var.updateRequiredBits(null, factorSize); if
		 * (factorSize.isZeroOrPMOne()){ //The size is unchanged } else { if
		 * (factorSize.isUnbounded()){ return; } na +=
		 * factorSize.getRequiredNumeratorBits(); nb +=
		 * factorSize.getRequiredDenominatorBits(); } } for(PolynomialExpression pm
		 * : polyFactors){ factorSize.setUnbounded(); pm.updateRequiredBits(null,
		 * factorSize); if (factorSize.isZeroOrPMOne()){ //The size is unchanged }
		 * else { if (factorSize.isUnbounded()){ return; } na +=
		 * factorSize.getRequiredNumeratorBits(); nb +=
		 * factorSize.getRequiredDenominatorBits(); } }
		 * 
		 * if (na == 0 && nb == 0){ //This occurs if all factors are zero or pm one
		 * termSize.setZeroOrPMOne(); } //A numerator or denominator, alone, being
		 * zero or pm one does nothing for us. if (na == 0){ na = 1; } if (nb == 0){
		 * nb = 1; } termSize.updateRequiredBits(na, nb); }
		 */
		/**
		 * Returns this term, cast as a ConstExpression or an LvalExpression, if
		 * possible. If this is not possible, null is returned.
		 */
		public Expression toLvalConst() {
			if (getDegree() == 0) {
				return constant;
			}
			if (getNumMonomerFactors() == 1 && getNumPolynomialFactors() == 0
			    && constant.isOne()) {
				return monomerFactors.get(0).var;
			}
			return null;
		}

		/**
		 * Only if this term does not have any polynomial expression factors, print
		 * it out.
		 */
		public void toCircuit(Object object, PrintWriter circuit) {
			if (monomerFactors.isEmpty() && polyFactors.isEmpty()) {
				constant.toCircuit(null, circuit); // just the constant.
			} else {
				if (constant.isOne()) {
					// multiplying by one is identity
				} else if (constant.isNegOne()) {
					circuit.print(" - "); // multiplying by neg one is defined as negation
				} else {
					constant.toCircuit(null, circuit);
					circuit.print(" * "); // general case
				}
			}
			for (Iterator<PolynomialMonomer> itr2 = monomerFactors.iterator(); itr2
			    .hasNext();) {
				PolynomialMonomer pm = itr2.next();
				pm.var.toCircuit(null, circuit);
				if (itr2.hasNext() || !polyFactors.isEmpty()) {
					circuit.print(" * ");
				}
			}
			for (Iterator<PolynomialExpression> itr2 = polyFactors.iterator(); itr2
			    .hasNext();) {
				circuit.print("( ");
				PolynomialExpression pe = itr2.next();
				for (Iterator<PolynomialTerm> itr3 = pe.terms.iterator(); itr3
				    .hasNext();) {
					PolynomialTerm pt = itr3.next();
					pt.toCircuit(null, circuit);
					if (itr3.hasNext()) {
						circuit.print(" + ");
					}
				}
				circuit.print(" )");
				if (itr2.hasNext()) {
					circuit.print(" * ");
				}
			}
		}

		private Collection<LvalExpression> getBooleanLvalExpressionInputs() {
			ArrayList<LvalExpression> toRet = new ArrayList();
			for (PolynomialMonomer pm : monomerFactors) {
				if (TypeHeirarchy.isSubType(pm.var.getType(), new BooleanType())) {
					toRet.add(pm.var);
				}
			}
			for (PolynomialExpression pe : polyFactors) {
				toRet.addAll(pe.getBooleanLvalExpressionInputs());
			}
			return toRet;
		}

		public Vector getLvalExpressionInputs() {
			Vector toRet = new Vector();
			for (PolynomialMonomer pm : monomerFactors) {
				toRet.add(pm.var);
			}
			for (PolynomialExpression pe : polyFactors) {
				toRet.addAll(pe.getLvalExpressionInputs());
			}
			return toRet;
		}

		/*
		 * Returns true if this term is of the form (linear) * (linear)
		 */
		public boolean isProductOfTwoLinear() {
			// if (!constant.isOne()){
			// return false;
			// }
			int linearFactors = 0;
			linearFactors += monomerFactors.size();
			for (PolynomialExpression pe : polyFactors) {
				// pe must be linear
				if (pe.getDegree() != 1) {
					return false;
				}
				linearFactors++;
			}
			return linearFactors <= 2;
		}

		/**
		 * If the constant is one, do nothing. Otherwise, take the first factor and
		 * multiply it by the constant (this may turn a monomial into a polynomial
		 * factor)
		 */
		public void toUnitConstantForm() {
			if (constant.isOne()) {
				return;
			}
			if (constant.isZero()) {
				throw new RuntimeException("Assertion error");
			}
			if (monomerFactors.size() > 0) {
				// Change a monomer to a poly
				PolynomialMonomer rem = monomerFactors.remove(0);
				PolynomialExpression rep = new PolynomialExpression();
				PolynomialTerm pt = new PolynomialTerm();
				pt.addFactor(constant);
				pt.addFactor(rem);
				rep.addMultiplesOfTerm(IntConstant.ONE, pt);
				polyFactors.add(rep);
			} else if (polyFactors.size() > 0) {
				// throw new
				// RuntimeException("The compiler shall not distribute a constant through a linear function.");
				// Scale the with fewest terms
				int minTerms = Integer.MAX_VALUE;
				PolynomialExpression minPe = null;
				for (PolynomialExpression pe : polyFactors) {
					if (pe.getTerms().size() < minTerms) {
						minTerms = pe.getTerms().size();
						minPe = pe;
					}
				}
				for (PolynomialTerm pt : minPe.getTerms()) {
					pt.constant = pt.constant.multiply(constant);
				}
			} else {
				// Probably not what you want.
				throw new RuntimeException("Constant expression " + constant
				    + " could not be coerced to a unit constant");
			}
			constant = FloatConstant.ONE;
			Collections.sort(monomerFactors);
			Collections.sort(polyFactors);
			simplifyProduct();
		}

		/*
		 * public PolynomialExpression expandProduct() { List<PolynomialTerm> terms
		 * = new ArrayList(); terms.add(new PolynomialTerm()); for(PolynomialMonomer
		 * pm : monomerFactors){ for(PolynomialTerm pt : terms){;
		 * pt.addFactor(pm.var); } } for(PolynomialExpression pe : polyFactors){
		 * List<PolynomialTerm> newTerms = new ArrayList(); for(PolynomialTerm pt :
		 * pe.getTerms()){ PolynomialExpression pe2 = pt.expandProduct();
		 * for(PolynomialTerm pt2 : pe2.getTerms()){ for(PolynomialTerm pt3 :
		 * terms){ PolynomialTerm product = PolynomialTerm.productTerm(pt2, pt3);
		 * newTerms.add(product); } } } terms = newTerms; } PolynomialExpression
		 * toRet = new PolynomialExpression(); for(PolynomialTerm pt : terms){
		 * toRet.addMultiplesOfTerm(constant, pt); } return toRet; }
		 */

		/**
		 * Ensure that any sub-polynomial factors of this term have do not contain
		 * fractional constants.
		 */
		public void removeFractionalConstants() {
			for (PolynomialExpression pe : polyFactors) {
				IntConstant denom = pe.removeFractionalConstants();
				constant = constant.multiply(FloatConstant.valueOf(1, denom.value()));
			}
		}

		/**
		 * Get the type of this term when certain variables are filled in with
		 * integer values.
		 */
		public PolynomialTerm evaluate(int[] vals,
		    HashMap<Integer, Integer> outputLineToVarNum) {
			PolynomialTerm toRet = new PolynomialTerm();
			for (PolynomialMonomer f : monomerFactors) {
				int fnum = lvalToNum(f.var);
				if (outputLineToVarNum.containsKey(fnum)) {
					int x = vals[outputLineToVarNum.get(lvalToNum(f.var))];
					toRet.addFactor(IntConstant.valueOf(x));
				} else {
					toRet.addFactor(f);
				}
			}
			for (PolynomialExpression pe : polyFactors) {
				toRet.addFactor(pe.evaluate(vals, outputLineToVarNum));
			}
			toRet.constant = toRet.constant.multiply(constant);
			return toRet;
		}

		public Expression toOperationExpression() {
			Expression expr = constant;
			for (PolynomialMonomer pm : monomerFactors) {
				expr = new BinaryOpExpression(new TimesOperator(), expr, pm.var);
			}
			for (PolynomialExpression pe : polyFactors) {
				Expression ft = pe.toOperationExpression();
				expr = new BinaryOpExpression(new TimesOperator(), expr, ft);
			}
			return expr;
		}
	}

	// expand the polynomial back to operation expression for purpose of point-to
	// analysis
	public Expression toOperationExpression() {
		Expression expr = null;
		for (PolynomialTerm pt : terms) {
			if (expr == null) {
				expr = new UnaryOpExpression(new UnaryPlusOperator(),
				    pt.toOperationExpression());
			} else {
				expr = new BinaryOpExpression(new PlusOperator(), expr,
				    pt.toOperationExpression());
			}
		}
		if (expr == null) {
			expr = IntConstant.ZERO;
		}
		return expr;
	}

	/**
	 * 
	 * Returns null if coercion is not possible
	 */
	public static PolynomialExpression toPolynomialExpression(Expression expr) {
		if (expr instanceof PolynomialExpression) {
			return (PolynomialExpression) expr;
		}
		ConstExpression asConst = ConstExpression.toConstExpression(expr);
		if (asConst != null) {
			PolynomialExpression pe = new PolynomialExpression();
			PolynomialTerm pt = new PolynomialTerm();
			pe.addMultiplesOfTerm(asConst, pt);
			return pe;
		}
		Pointer asPointer = Pointer.toPointerConstant(expr);
		if (asPointer != null) {
			PolynomialExpression pe = new PolynomialExpression();
			PolynomialTerm pt = new PolynomialTerm();
			pe.addMultiplesOfTerm(asPointer.value(), pt);
			return pe;
		}
		LvalExpression asLval = LvalExpression.toLvalExpression(expr);
		if (asLval != null) {
			PolynomialTerm pt = new PolynomialTerm();
			// Create a simple 1*x polynomial to return the value.
			PolynomialExpression pe = new PolynomialExpression();
			PolynomialMonomer pm = new PolynomialMonomer(asLval);
			pt.addFactor(pm);
			pe.addMultiplesOfTerm(IntConstant.ONE, pt);
			return pe;
		}

		if (expr instanceof UnaryOpExpression) {
			UnaryOpExpression uo = ((UnaryOpExpression) expr);
			PolynomialExpression middle = PolynomialExpression
			    .toPolynomialExpression(uo.getMiddle());
			if (uo.getOperator() instanceof UnaryPlusOperator) {
				return middle;
			}
			if (uo.getOperator() instanceof UnaryMinusOperator) {
				PolynomialExpression pe = new PolynomialExpression();
				PolynomialTerm pt = new PolynomialTerm();
				pt.addFactor(middle);
				pe.addMultiplesOfTerm(IntConstant.NEG_ONE, pt);
				return pe;
			}
			return toPolynomialExpression(uo.getOperator().resolve(uo.getMiddle()));
		}
		if (expr instanceof BinaryOpExpression) {
			BinaryOpExpression bo = ((BinaryOpExpression) expr);
			PolynomialExpression left = PolynomialExpression
			    .toPolynomialExpression(bo.getLeft());
			PolynomialExpression right = PolynomialExpression
			    .toPolynomialExpression(bo.getRight());

			if (left != null && right != null) {
				PolynomialExpression pe = new PolynomialExpression();

				if (bo.getOperator() instanceof PlusOperator) {
					PolynomialTerm p1 = new PolynomialTerm();
					PolynomialTerm p2 = new PolynomialTerm();
					p1.addFactor(left);
					p2.addFactor(right);
					pe.addMultiplesOfTerm(IntConstant.ONE, p1);
					pe.addMultiplesOfTerm(IntConstant.ONE, p2);
					return pe;
				}
				if (bo.getOperator() instanceof TimesOperator) {
					PolynomialTerm p1 = new PolynomialTerm();
					p1.addFactor(left);
					p1.addFactor(right);
					pe.addMultiplesOfTerm(IntConstant.ONE, p1);
					return pe;
				}
				if (bo.getOperator() instanceof MinusOperator) {
					PolynomialTerm p1 = new PolynomialTerm();
					PolynomialTerm p2 = new PolynomialTerm();
					p1.addFactor(left);
					p2.addFactor(right);
					pe.addMultiplesOfTerm(IntConstant.ONE, p1);
					pe.addMultiplesOfTerm(IntConstant.NEG_ONE, p2);
					return pe;
				}
			}

			return toPolynomialExpression(bo.getOperator().resolve(bo.getLeft(),
			    bo.getRight()));
		}

		if (expr instanceof BitString) {
			BitString bs = (BitString) expr;
			Type bitwiseEncoding = bs.getBitwiseEncoding();

			int N = IntType.getBits((IntType) bitwiseEncoding);
			boolean signed = bitwiseEncoding instanceof RestrictedSignedIntType;

			final Type targetType;
			if (signed) {
				targetType = new RestrictedSignedIntType(N);
			} else {
				targetType = new RestrictedUnsignedIntType(N);
			}
			PolynomialExpression pe = new PolynomialExpression() {
				public Type getType() {
					// We have additional information about this polynomial (but still,
					// the super implementation may do better
					// if many of the upper terms are 0's)
					return TypeHeirarchy.looseIntersect(targetType, super.getType());
				}
			};
			BigInteger pot = BigInteger.ONE;
			for (int i = 0; i < N; i++, pot = pot.shiftLeft(1)) {
				BigInteger signedPot = pot;
				if (i == N - 1 && signed) {
					signedPot = signedPot.negate();
				}
				PolynomialTerm pt = new PolynomialTerm();
				pt.addFactor(bs.fieldEltAt(i));
				pe.addMultiplesOfTerm(IntConstant.valueOf(signedPot), pt);
			}
			return pe;
		}

		// Couldn't coerce.
		return null;
	}

	private static class TermsList extends ArrayList<PolynomialTerm> {
		private int degree = 0;
		// It's O.K. to lable lvals as inputs that aren't inputs any more (i.e. they
		// got cancelled)
		private HashSet<LvalExpression> lvalInputs = new HashSet();
		private HashSet<LvalExpression> booleanLvalExpressionInputs = new HashSet();
		private Type componentType = new BooleanType();

		public PolynomialTerm floor(PolynomialTerm pt) {
			int bst = search_(pt);

			if (bst < 0) {
				throw new RuntimeException("Not found");
			} else {
				return get(bst);
			}
		}

		private int search_(PolynomialTerm pt) {
			// Optimization: if pt is > the last element, return -size() - 1
			if (isEmpty() || get(size() - 1).compareTo(pt) < 0) {
				return -size() - 1;
			}
			return Collections.binarySearch(this, pt);
		}

		public boolean add(PolynomialTerm e) {
			// update lval inputs
			lvalInputs.addAll(e.getLvalExpressionInputs());

			booleanLvalExpressionInputs.addAll(e.getBooleanLvalExpressionInputs());
			degree = Math.max(degree, e.getDegree());

			// update type
			componentType = TypeHeirarchy.looseUnion(componentType, e.getType());

			if (isEmpty()) {
				super.add(0, e);
				return true;
			}
			int bst = search_(e);
			if (bst < 0) {
				bst = -(bst + 1);
			} else {
				throw new RuntimeException("Collision");
			}
			super.add(bst, e);

			/*
			 * for(PolynomialTerm pt : this){ PrintWriter circuit = new
			 * PrintWriter(System.out); pt.toCircuit(null, circuit); circuit.flush();
			 * System.out.print(" "); } System.out.println(" ");
			 */
			return true;
		}

		public boolean contains(Object o) {
			if (isEmpty()) {
				return false;
			}
			int bst = search_((PolynomialTerm) o);
			return bst >= 0;
		}

		public boolean remove(Object o) {
			int bst = search_((PolynomialTerm) o);

			remove(bst);

			return true;
		}

		public Collection<LvalExpression> getLvalExpressionInputs() {
			return lvalInputs;
		}

		public Collection<LvalExpression> getBooleanLvalExpressionInputs() {
			return booleanLvalExpressionInputs;
		}
	}

	// private TreeSet<PolynomialTerm> terms = new TreeSet();

	private TermsList terms;

	public List<PolynomialTerm> getTerms() {
		return terms;
	}

	public PolynomialExpression() {
		super(new PolynomialOperator());
		terms = new TermsList();
		// terms = new TreeSet();
	}

	public Collection<LvalExpression> getBooleanLvalExpressionInputs() {
		return terms.getBooleanLvalExpressionInputs();
	}

	public Collection<LvalExpression> getLvalExpressionInputs() {
		return terms.getLvalExpressionInputs();
	}

	public PolynomialExpression duplicate() {
		PolynomialExpression pe = new PolynomialExpression();
		for (PolynomialTerm pt : terms) {
			pe.terms.add(pt.duplicate());
		}
		return pe;
	}

	/**
	 * Computes total degre of this polynomial
	 */
	public int getDegree() {
		return terms.degree;
	}

	/**
	 * Assumes that the input collection of terms are all disjoint. public void
	 * addDisjointTerms(Collection<PolynomialTerm> terms) { for(PolynomialTerm pt
	 * : terms){ addMultiplesOfTerm(IntConstant.ONE, pt); } }
	 */

	public void addMultiplesOfTerm(ConstExpression multiple, PolynomialTerm pt) {
		// Check if pt is a sum of terms. If so, recursively add those terms
		// (scaled)
		if (pt.monomerFactors.isEmpty() && pt.polyFactors.size() == 1) {
			FloatConstant newMultiple = FloatConstant.toFloatConstant(multiple)
			    .multiply(pt.constant);
			for (PolynomialTerm pt2 : pt.polyFactors.get(0).terms) {
				addMultiplesOfTerm(newMultiple, pt2); // scale by newMultiple
			}
			return;
		}

		FloatConstant multiple_ = FloatConstant.toFloatConstant(multiple);
		FloatConstant preMultiply = multiple_.multiply(pt.constant);
		if (preMultiply.isZero()) {
			return;
		}
		if (terms.contains(pt)) {
			pt = terms.floor(pt); // ordering is unique, but we have to do it this
			                      // way.
			terms.remove(pt);
			PolynomialTerm newTerm = pt.duplicate();
			newTerm.constant = preMultiply.add(pt.constant);
			pt = newTerm;
			if (!pt.constant.isZero()) {
				terms.add(pt);
			}
		} else {
			PolynomialTerm newTerm = pt.duplicate();
			newTerm.constant = preMultiply;
			terms.add(newTerm);
		}
	}

	public void addProductOfTerms(PolynomialTerm ptL, PolynomialTerm ptR) {
		PolynomialTerm product = PolynomialTerm.productTerm(ptL, ptR);
		addMultiplesOfTerm(IntConstant.ONE, product);
	}

	public Expression changeReference(VariableLUT unique) {
		System.err.println("Warning - change references on a polynomial");
		// throw new RuntimeException("change reference not implemented");
		return this;
	}

	public BlockStatement toSLPTCircuit(Object obj) {
		AssignmentStatement as = ((AssignmentStatement) obj);
		LvalExpression lhs = as.getLHS(); // LHS of the param statement
		BlockStatement result = new BlockStatement();

		if (lhs.size() != 1) {
			throw new RuntimeException(
			    "Assigning polynomial expression to value of size != 1");
		}

		result.addStatement(new AssignmentStatement(lhs.lvalFieldEltAt(0), this));
		return result;
	}

	public String toString() {
		StringWriter wr = new StringWriter();
		toCircuit(null, new PrintWriter(wr));
		return wr.toString();
	}

	public void toCircuit(Object obj, PrintWriter circuit) {
		// Output it in a factorization that's easy for the backend to understand.
		// Notation: ( ) is the empty polynomial, which is equal to 0. So ( ) * a =
		// 0.
		// This is to make it easy to count nonzero terms, because 0 is never listed
		// as a term in the polynomials.

		IntConstant fractionCommonDenominator = IntConstant.ONE;
		if (AssignmentStatement.removeFractions) {
			fractionCommonDenominator = removeFractionalConstants();
		}
		FloatConstant multiplier = FloatConstant.valueOf(1,
		    fractionCommonDenominator.value());
		multiplier.toCircuit(null, circuit);
		circuit.print(" * ( ");

		switch (Optimizer.optimizationTarget) {
		case ZAATAR: {
			Iterator<PolynomialTerm> itr = terms.iterator();
			if (getDegree() == 2) {
				PolynomialTerm d2t = itr.next(); // assumption: d2t is product of linear
				// Duplicate and make it unit form:
				d2t = d2t.duplicate();
				d2t.toUnitConstantForm(); // now d2t is product of linear
				for (Iterator<PolynomialMonomer> itr2 = d2t.monomerFactors.iterator(); itr2
				    .hasNext();) {
					PolynomialMonomer pm = itr2.next();
					circuit.print("( ");
					pm.var.toCircuit(null, circuit);
					circuit.print(" )");
					if (itr2.hasNext() || !d2t.polyFactors.isEmpty()) {
						circuit.print(" * ");
					}
				}
				for (Iterator<PolynomialExpression> itr2 = d2t.polyFactors.iterator(); itr2
				    .hasNext();) {
					PolynomialExpression pe = itr2.next(); // .expandProducts();
					circuit.print("( ");
					for (Iterator<PolynomialTerm> itr3 = pe.terms.iterator(); itr3
					    .hasNext();) {
						PolynomialTerm pt = itr3.next();
						// pt is expanded
						pt.toCircuit(null, circuit);
						if (itr3.hasNext()) {
							circuit.print(" + ");
						}
					}
					circuit.print(" )");
					if (itr2.hasNext()) {
						circuit.print(" * ");
					}
				}
			} else {
				circuit.print("( ) * ( ) ");
			}
			circuit.print(" + ( ");
			/*
			 * PolynomialExpression C = new PolynomialExpression(); for( ;
			 * itr.hasNext(); ){ PolynomialExpression pe = itr.next().expandProduct();
			 * for(Iterator<PolynomialTerm> itr2 = pe.terms.iterator();
			 * itr2.hasNext(); ){ C.addMultiplesOfTerm(IntConstant.ONE, itr2.next());
			 * } } for(Iterator<PolynomialTerm> itr2 = C.terms.iterator();
			 * itr2.hasNext(); ){
			 */
			while (itr.hasNext()) {
				PolynomialTerm pt = itr.next();
				pt.toCircuit(null, circuit);
				if (itr.hasNext()) {
					circuit.print(" + ");
				}
			}
			circuit.print(" )");
		}
			break;
		case GINGER:
			/*
			 * PolynomialExpression expanded = expandProducts();
			 * for(Iterator<PolynomialTerm> itr2 = expanded.getTerms().iterator();
			 * itr2.hasNext(); ){ itr2.next().toCircuit(null, circuit); if
			 * (itr2.hasNext()){ circuit.print(" + "); } }
			 */
			for (Iterator<PolynomialTerm> itr2 = terms.iterator(); itr2.hasNext();) {
				itr2.next().toCircuit(null, circuit);
				if (itr2.hasNext()) {
					circuit.print(" + ");
				}
			}
			break;
		default:
			throw new RuntimeException("Unrecognized output target");
		}
		circuit.print(" )");
	}

	/**
	 * Expand this polynomial expression private PolynomialExpression
	 * expandProducts() { PolynomialExpression toRet = new PolynomialExpression();
	 * for(PolynomialTerm pt : terms){ PolynomialExpression sub =
	 * pt.expandProduct(); for (PolynomialTerm pt2 : sub.terms){
	 * toRet.addMultiplesOfTerm(IntConstant.ONE, pt2); } }
	 * 
	 * return toRet; }
	 */

	/**
	 * Multiplies this polynomial by a suitable integer constant, C, such that no
	 * fractional constants remain in the expression, and then C is returned.
	 */
	private IntConstant removeFractionalConstants() {
		IntConstant denom_sum = IntConstant.ONE;
		for (PolynomialTerm pt : terms) {
			// Remove fractions from subpolynomials
			pt.removeFractionalConstants();

			IntConstant denom = IntConstant.valueOf(pt.constant.getDenominator());

			if (!denom.isPOT()) {
				throw new RuntimeException("Non power of two denominator: " + denom);
			}
			if (denom.compareTo(denom_sum) > 0) {
				denom_sum = denom;
			}
		}
		// Now multiply through by denom_sum and return denom_sum
		for (PolynomialTerm pt : terms) {
			BigInteger num = pt.constant.getNumerator();
			BigInteger den = pt.constant.getDenominator();
			pt.constant = FloatConstant.valueOf(num.multiply(denom_sum.value())
			    .divide(den), 1);
		}

		return denom_sum;
	}

	/**
	 * Compare by degree (higher degree first), then number of terms (higher
	 * first), then lexicographically sort terms.
	 */
	public int compareTo(PolynomialExpression o) {
		int compareDegree = o.getDegree() - getDegree();
		if (compareDegree == 0) {
			int compareNumTerms = o.getTerms().size() - getTerms().size();
			if (compareNumTerms == 0) {
				Iterator<PolynomialTerm> i1 = getTerms().iterator();
				Iterator<PolynomialTerm> i2 = o.getTerms().iterator();
				while (i1.hasNext()) {
					PolynomialTerm pt1 = i1.next();
					PolynomialTerm pt2 = i2.next();
					int cmp = pt1.compareTo(pt2);
					if (cmp != 0) {
						return cmp;
					}
					// compare constants if terms are equal
					cmp = pt1.constant.compareTo(pt2.constant);
					if (cmp != 0) {
						return cmp;
					}
				}
				return 0; // equal polynomial expressions
			}
			return compareNumTerms;
		}
		return compareDegree;
	}

	public Expression inline(Object obj, StatementBuffer assignments) {
		// Currently, we don't have any patterns that start at a general polynomial
		// and work up.
		// Thus, it's useful to use the BinaryOperations where possible because they
		// have stronger optimization features

		// Polynomial "empty"
		if (terms.size() == 0) {
			return IntConstant.ZERO;
		}
		if (terms.size() == 1) {
			// Polynomial "constant"
			for (PolynomialTerm term : terms) {
				if (term.getDegree() == 0) {
					return term.constant;
				}
				// Polynomial "x"
				if (term.getNumPolynomialFactors() == 0 && term.getDegree() == 1
				    && (term.constant.isOne())) {
					return term.getMonomerFactor(0);
				}
			}
		}

		return this;
	}

	public Type getType() {
		// Can we substitute booleans to determine the type of this polynomial?
		// This provides the exact type of the polynomial, but not generally
		// tractible
		Type exactType = getTypeByBooleanSubstitution();
		if (exactType != null) {
			return exactType;
		}

		// Approximate the types of the terms, and if they are all integer types,
		// we can use interval arithmetic to approximate the type for this
		// polynomial
		Type intervalType = getTypeByIntervalArithmetic();
		if (intervalType != null) {
			return intervalType;
		}

		// Otherwise, use a very coarse approximation which homogenizes all terms
		// with a union.
		return PlusOperator.sumTypes(terms.size(), terms.componentType);
	}

	private Type getTypeByIntervalArithmetic() {
		if (terms.size() <= 4) {
			boolean allInts = true;
			ArrayList<RestrictedIntType> types = new ArrayList();
			for (PolynomialTerm pt : terms) {
				Type type = pt.getType();
				if (!(type instanceof RestrictedIntType)) {
					allInts = false;
					break;
				}
				types.add((RestrictedIntType) type);
			}
			if (allInts) {
				IntConstantInterval ici = new IntConstantInterval(IntConstant.ZERO,
				    IntConstant.ZERO);
				for (RestrictedIntType r : types) {
					ici = ici.add(r.getInterval());
				}
				if (ici.lower.compareTo(IntConstant.ZERO) < 0) {
					return new RestrictedSignedIntType(ici);
				} else {
					return new RestrictedUnsignedIntType(ici);
				}
			}
		}
		return null;
	}

	/**
	 * Return true if an exhaustive search over the inputs is performed to find
	 * the required bits to hold the result of this polynomial.
	 * 
	 * The current implementation only performs the exhaustive search if the
	 * search will terminate relatively quickly. Currently: - If any input to the
	 * polynomial is not a boolean or a zero_or_pm_one value, the search is not
	 * performed - If there are more than 10 inputs, the search is not performed
	 * 
	 * If there are no inputs to the expression, then exhaustive search is also
	 * not performed.
	 * 
	 * Returns null if the search is not performed or the search is inconclusive.
	 */
	private Type getTypeByBooleanSubstitution() {
		Collection<LvalExpression> booleanInputs = getBooleanLvalExpressionInputs();
		int N = booleanInputs.size();
		if (N <= 0) {
			return null;
		}
		if (N > 10) {
			return null;
		}
		int[] minVal = new int[N];
		int[] maxVal = new int[N];
		HashMap<Integer, Integer> outputLineToVarNum = new HashMap();

		int i = 0;
		for (LvalExpression vEx : booleanInputs) {
			minVal[i] = 0;
			maxVal[i] = 1;
			int outputLine = lvalToNum(vEx);
			outputLineToVarNum.put(outputLine, i);
			i++;
		}

		// Exhaustive evaluation of the polynomial
		int p = 0;
		int[] vals = new int[N];
		vals[p] = minVal[p];

		Type foundType = null;
		while (p >= 0) {
			if (vals[p] > maxVal[p]) {
				p--;
				if (p >= 0) {
					vals[p]++;
				}
				continue;
			}
			if (p < N - 1) {
				p++;
				vals[p] = minVal[p];
			}
			if (p == N - 1) {
				// Evaluate this assignment
				PolynomialExpression localEval = evaluate(vals, outputLineToVarNum);
				Type result = localEval.getType();

				if (foundType == null) {
					foundType = result;
				} else {
					foundType = TypeHeirarchy.looseUnion(foundType, result);
				}

				// Go to next assignment
				vals[p]++;
			}
		}

		return foundType;
	}

	/*
	 * private Collection<LvalExpression> booleanLvals(Collection<LvalExpression>
	 * lvals) { List<LvalExpression> toRet = new ArrayList(); for(LvalExpression
	 * lv : lvals){ if (TypeHeirarchy.isSubType(lv.getType(), new BooleanType())){
	 * toRet.add(lv); } } return toRet; }
	 */

	/**
	 * Perform variable substitution on this polynomial.
	 */
	private PolynomialExpression evaluate(int[] vals,
	    HashMap<Integer, Integer> outputLineToVarNum) {
		PolynomialExpression toRet = new PolynomialExpression();
		for (PolynomialTerm pt : terms) {
			toRet.addMultiplesOfTerm(IntConstant.ONE,
			    pt.evaluate(vals, outputLineToVarNum));
		}
		// Re-form the polynomial to account for cancellation.
		toRet = toRet.duplicate();
		return toRet;
	}

	private static int lvalToNum(LvalExpression vEx) {
		StatementWithOutputLine as = vEx.getAssigningStatement();
		int outputLine = -1;
		if (as instanceof InputStatement) {
			outputLine = ((InputStatement) as).getOutputLine();
		}
		// if (as instanceof AssignmentStatement) {
		outputLine = as.getOutputLine();
		// }

		if (outputLine < 0) {
			throw new RuntimeException("Lvalue " + vEx.getName()
			    + " has no output line in polynomial");
		}
		return outputLine;
	}

	public OperationExpression sortInputs() {
		// Inputs are sorted by default.
		return this;
	}

	public int size() {
		return 1;
	}

	public Expression fieldEltAt(int i) {
		if (i != 0) {
			throw new RuntimeException("Array index out of bounds index " + i
			    + " fieldEltAt in polynomial " + this);
		}
		return this;
	}
}
