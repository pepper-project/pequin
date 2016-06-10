package SFE.Compiler;

import java.util.ArrayList;
import java.util.HashSet;

import SFE.Compiler.Operators.CStyleCast;
import SFE.Compiler.Operators.Operator;
import SFE.Compiler.Operators.PointerAccessOperator;
import SFE.Compiler.Operators.UnaryPlusOperator;

/**
 * A proto-assignment statement is an assignment of the form LHS = RHS;
 * 
 * or potentially
 * 
 * LHS (op)= RHS
 * 
 * where LHS is an expression which resolves to an Lval and RHS is an
 * expression. During expansion to assignment statements, the LHS is looked up
 * first, the RHS is then looked up, and then an assignment between the two
 * results is emitted. op is a binaryOperator.
 */
public class ProtoAssignmentStatement extends Statement {
	private Expression lhsLookup;
	private Expression rhs;
	private Operator binaryOperator;
	private CStyleCast cast;

	private LvalExpression resolvedLHS; // Can be queried by expressions after
	                                    // toAssignmentStatements has been called

	public ProtoAssignmentStatement(Expression lhsLookup, Expression rhs,
	    CStyleCast castResult) {
		this.lhsLookup = lhsLookup;
		this.rhs = rhs;
		this.cast = castResult;
	}

	public ProtoAssignmentStatement(Expression lhsLookup, Expression rhs,
	    Operator binaryOperator, CStyleCast castResult) {
		this.lhsLookup = lhsLookup;
		this.rhs = rhs;
		this.binaryOperator = binaryOperator;
		this.cast = castResult;
	}

	public Statement toSLPTCircuit(Object obj) {
		return this;
	}

	public Statement duplicate() {
		return new ProtoAssignmentStatement(lhsLookup.duplicate(), rhs.duplicate(),
		    binaryOperator, cast);
	}

	public static int ramOperationCounter = 0;

	public void toAssignmentStatements(StatementBuffer assignments) {
		// Apply unique vars to LHS lookup
		lhsLookup = lhsLookup.changeReference(Function.getVars());
		// Lookup the lhs
		resolvedLHS = LvalExpression.toLvalExpression(lhsLookup);

		Expression addr = null;
		// LvalExpression base = null;
		HashSet<Integer> possibleAddresses = new HashSet<Integer>();
		// if lhs cannot be resolved, generate ramput to store the rhs to lhs's
		// address
		// in this case, lhs should be a pointer/struct that cannot be resolved at
		// compile time.
		if (resolvedLHS == null) {
			BlockStatement result = new BlockStatement();

			// base = PointerAccessOperator.getBaseAddress(lhsLookup);
			addr = PointerAccessOperator.asAddress(lhsLookup, result,
			    possibleAddresses);
			addr = addr.evaluateExpression("ramput", "addr" + ramOperationCounter,
			    result);
			resolvedLHS = Function.addTempLocalVar(
			    "ramput:lhs" + ramOperationCounter, lhsLookup.getType());
			ramOperationCounter++;
			result.toAssignmentStatements(assignments);
		}

		if (resolvedLHS == null) {
			throw new RuntimeException("I don't know how to resolve lhs. "
			    + lhsLookup);
		}
		/*
		 * if (resolvedLHS.getAssigningStatement() == null){ //Zero out the
		 * variable, as this is the first assignment to it. throw new
		 * RuntimeException
		 * ("Assignment to uninitialized variable in protoAssignmentStatement"); }
		 */
		// Do we have an operation to perform?
		if (binaryOperator != null) {
			// if so, and lhs needs to be accessed using a ram operations, generate a
			// ramget to fetch its value to be used in the computation.
			if (addr != null) {
				BlockStatement cacheManagementStatement = new BlockStatement();
				for (int possibleAddress : possibleAddresses) {
					for (int i = 0; i < resolvedLHS.size(); i++) {
						cacheManagementStatement.addStatement(CacheManager
						    .invalidateCache(possibleAddress + i));
					}
				}
				cacheManagementStatement.toSLPTCircuit(null);
				cacheManagementStatement.toAssignmentStatements(assignments);
				
				// generate a ramget statement
				Statement ramget = new RamGetEnhancedStatement(resolvedLHS, addr);
				ramget = ramget.toSLPTCircuit(null);
				ramget.toAssignmentStatements(assignments);
			}
			rhs = new BinaryOpExpression(binaryOperator, resolvedLHS, rhs);
		}
		if (cast != null) {
			rhs = new UnaryOpExpression(cast, rhs);
		}
		// Peel off compile-time-resolvable operations. Requires changeReference
		// first
		rhs = rhs.changeReference(Function.getVars());
		rhs = Expression.fullyResolve(rhs);
		// Peeling off may lead to a non-operation expression, make a dummy op if
		// this is the case
		if (!(rhs instanceof OperationExpression)) {
			rhs = new UnaryOpExpression(new UnaryPlusOperator(), rhs);
		}

		// Emit the AS, and process it to finish the job
		AssignmentStatement toRet = new AssignmentStatement(resolvedLHS, rhs);
		Statement slptCircuit = toRet.toSLPTCircuit(null);
		slptCircuit.toAssignmentStatements(assignments);

		if (addr != null) {
			// if lhs cannot be resolved at compile time, need to generate ram
			// operations to store the final result into ram.
			// invalidate cache for all entries this ramput might tamper.
			BlockStatement cacheManagementStatement = new BlockStatement();
			for (int possibleAddress : possibleAddresses) {
				for (int i = 0; i < resolvedLHS.size(); i++) {
					cacheManagementStatement.addStatement(CacheManager
					    .invalidateCache(possibleAddress + i));
				}
			}
			cacheManagementStatement.toSLPTCircuit(null);
			cacheManagementStatement.toAssignmentStatements(assignments);
			// generate ramput statement to store the results into RAM

			Statement ramput = new RamPutEnhancedStatement(addr, resolvedLHS,
			    assignments.getCondition(), assignments.getBranch());
			ramput = ramput.toSLPTCircuit(null);
			ramput.toAssignmentStatements(assignments);
		} else {
			// mark variable as cached.
			if (resolvedLHS.getLvalue().hasAddress()) {
				// the variable is now cached in resolvedLHS.
				CacheManager.addCache(resolvedLHS.getAddress());
			}
		}
	}

	public Expression getResolvedLHS() {
		Expression toRet = new UnaryOpExpression(new Operator() {
			public int arity() {
				throw new RuntimeException("Not implemented");
			}

			public int priority() {
				throw new RuntimeException("Not implemented");
			}

			public Type getType(Object obj) {
				throw new RuntimeException("Not implemented");
			}

			public Expression inlineOp(StatementBuffer assignments,
			    Expression... args) {
				return resolve(args);
			}

			public LvalExpression resolve(Expression... args) {
				if (resolvedLHS == null) {
					throw new RuntimeException(
					    "Resolve called too soon - lhsLookup not available");
				}
				return resolvedLHS.changeReference(Function.getVars());
			}

			public Expression fieldEltAt(Expression expression, int i) {
				throw new RuntimeException("Not yet implemented");
			}
		}, IntConstant.ZERO);
		toRet.metaType = lhsLookup.metaType;
		return toRet;
	}
}
