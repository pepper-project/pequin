package SFE.Compiler.Operators;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import SFE.Compiler.AnyType;
import SFE.Compiler.ArrayType;
import SFE.Compiler.AssignmentStatement;
import SFE.Compiler.BinaryOpExpression;
import SFE.Compiler.BlockStatement;
import SFE.Compiler.CacheManager;
import SFE.Compiler.Expression;
import SFE.Compiler.Function;
import SFE.Compiler.IntConstant;
import SFE.Compiler.IntConstantInterval;
import SFE.Compiler.IntType;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.Pointer;
import SFE.Compiler.PointerType;
import SFE.Compiler.PolynomialExpression;
import SFE.Compiler.RamGetEnhancedStatement;
import SFE.Compiler.RestrictedIntType;
import SFE.Compiler.SLPTReduction;
import SFE.Compiler.Statement;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.StatementWithOutputLine;
import SFE.Compiler.StructType;
import SFE.Compiler.Type;
import SFE.Compiler.UnaryOpExpression;
import SFE.Compiler.UnaryOperator;

/**
 * This corresponds to * operation in C
 * 
 */
public class PointerAccessOperator extends Operator implements UnaryOperator,
    SLPTReduction {
	public int arity() {
		return 1;
	}

	public int priority() {
		throw new RuntimeException("Not implemented");
	}

	public Type getType(Object obj) {
		// No information until we've resolved the pointer during inlining.
		// we can do something here...
		if (obj instanceof UnaryOpExpression) {
			UnaryOpExpression uoe = (UnaryOpExpression) obj;
			if (uoe.metaType != null) {
				return uoe.metaType;
			}
		}
		return new AnyType();
	}

	public Expression inlineOp(StatementBuffer assignments, Expression... args) {
		Expression resolvedPointer = resolve(args);
		if (resolvedPointer != null) {
			return resolvedPointer;
		}

		return null;
	}

	public LvalExpression resolve(Expression... args) {
		// try to access a pointer, first convert the arg to be a pointer.
		Pointer a = Pointer.toPointerConstant(args[0]);
		if (a != null) {
			LvalExpression pointTo = a.access();
			if (pointTo != null) {
				return pointTo.changeReference(Function.getVars());
			}
		}
		return null;
	}

	public Expression fieldEltAt(Expression expression, int i) {
		throw new RuntimeException("Not yet implemented");
	}

	@Override
	public Statement toSLPTCircuit(Object obj) {
		BlockStatement result = new BlockStatement();

		AssignmentStatement as = (AssignmentStatement) obj;
		LvalExpression lhs = as.getLHS();

		UnaryOpExpression rhs = (UnaryOpExpression) as.getRHS();
		lhs.setType(rhs.getType());

		// LvalExpression base = getBaseAddress(rhs);
		HashSet<Integer> possibleAddresses = new HashSet<Integer>();
		Expression address = asAddress(rhs, result, possibleAddresses);

		// before we perform ramget, we need to invalidate all cache related to
		// this
		// address
		BlockStatement cacheManagementStatement = new BlockStatement();
		for (int possibleAddress : possibleAddresses) {
			for (int i = 0; i < lhs.size(); i++) {
				cacheManagementStatement.addStatement(CacheManager
				    .invalidateCache(possibleAddress + i));
			}
		}
		cacheManagementStatement.toSLPTCircuit(null);
		result.addStatement(cacheManagementStatement);

		Statement ramget = new RamGetEnhancedStatement(lhs, address);
		ramget = ramget.toSLPTCircuit(null);
		result.addStatement(ramget);
		return result;
	}

	/*
	 * This method takes an expression that cannot be resolved at compile time,
	 * and try to evaluate an expression that calculates its RAM address.
	 * 
	 * This should handle the following situations:
	 * 
	 * 1) *p where a is a pointer. *p will be parsed to
	 * UOE(op=PointerAccessOperator, middle=p)
	 * 
	 * 2) p->f (i.e. (*p).f) where a is a pointer to a struct. p->f will be parsed
	 * to UOE(op=StructAccessOperator, middle=UOE(op=PointerAccessOperator,
	 * middle=p)
	 * 
	 * 3) a[x] (i.e. *(a+x)) where a is an array. a[x] will be parsed to
	 * UOE(op=PointerAccessOperator, middle=BOE(op=Plus, left=a, right=x))
	 * 
	 * Complexity may arouse when the above are mixed together. For example,
	 * a[x].f, *(p->f), a[x][y], *(a[x]), a->x[y].f and etc. We need to handle
	 * this systematically here.
	 */
	public static Expression asAddress(Expression expression,
	    BlockStatement result, Set<Integer> possibleAddresses) {
		HashSet<Expression> possibleAddressExpressions = new HashSet<Expression>();

		Expression address = asAddressInternal(expression, result,
		    possibleAddressExpressions);

		// evaluate possible addresses and convert them to constant values.
		for (Expression possibleAddressExpression : possibleAddressExpressions) {
			Expression e = Expression.fullyResolve(possibleAddressExpression);
			IntConstant c = IntConstant.toIntConstant(e);
			if (c != null) {
				possibleAddresses.add(c.toInt());
			} else {
				throw new RuntimeException("Assertion failure.");
			}
		}

		return address;
	}

	// this function also replaces getBaseAddress it also tries to figure out
	// where it can possibly point to.
	//
	// for an array of structs, this is very useful to avoid unnecessary cache
	// flushes. for example, struct t {int a, b;}, struct t array[4];
	// array[x].a can only point to 4 possible locations, instead of 8.
	//
	private static Expression asAddressInternal(Expression expression,
	    BlockStatement result, Set<Expression> possibleAddresses) {
		LvalExpression lval = LvalExpression.toLvalExpression(expression);
		if (lval != null) {
			if (lval.hasAddress()) {
				possibleAddresses.add(IntConstant.valueOf(lval.getAddress()));
				return IntConstant.valueOf(lval.getAddress());
			}
		}
		if (expression instanceof UnaryOpExpression) {
			UnaryOpExpression uoe = (UnaryOpExpression) expression;
			Operator op = uoe.getOperator();
			Expression middle = uoe.getMiddle();
			if (op instanceof PointerAccessOperator) {
				Expression address = middle;
				if (address.metaType instanceof ArrayType) {
					if (address instanceof BinaryOpExpression) {
						// array accessing, possibly multiple dimentional
						BinaryOpExpression boe = (BinaryOpExpression) address;
						Expression arrayBaseAddress = asAddressInternal(boe.getLeft(),
						    result, possibleAddresses);

						BinaryOpExpression newBOE = new BinaryOpExpression(boe.op,
						    arrayBaseAddress, new BinaryOpExpression(new TimesOperator(),
						        IntConstant.valueOf(uoe.metaType.size()), boe.getRight()));

						newBOE.metaType = address.metaType;
						address = newBOE;

						// replace undecidable array indices with constant
						// integer
						HashSet<Expression> newPossibleAddress = new HashSet<Expression>();
						IntConstant offset = IntConstant.toIntConstant(boe.getRight());
						for (Expression addressExpression : possibleAddresses) {
							if (offset == null) {
								for (int i = 0; i < ((ArrayType) address.metaType).getLength(); i++) {
									newPossibleAddress.add(new BinaryOpExpression(boe.op,
									    addressExpression, new BinaryOpExpression(
									        new TimesOperator(), IntConstant.valueOf(uoe.metaType
									            .size()), IntConstant.valueOf(i))));
								}
							} else {
								newPossibleAddress.add(new BinaryOpExpression(boe.op,
								    addressExpression, new BinaryOpExpression(
								        new TimesOperator(), IntConstant.valueOf(uoe.metaType
								            .size()), offset)));
							}
						}
						possibleAddresses.clear();
						possibleAddresses.addAll(newPossibleAddress);

						return address;
					} else {
						throw new RuntimeException("I don't know how to interpret "
						    + expression + " as an address.");
					}
				} else if (address.metaType instanceof PointerType) {
					// this should be a pointer dereferencing, but the pointer
					// might not be compile time resolvable
					// in this case, we may need to use result to generate
					// ramget operations
					// TODO support multiple levels of indirection. i.e. **p
					// where p is a pointer to pointer.
					
					evaluatePossibleValues(middle, possibleAddresses, 0);
					return middle;
				} else {
					throw new RuntimeException("I don't know how to interpret "
					    + expression + " as an address.");
				}
			} else if (op instanceof StructAccessOperator) {
				// the challenge is to get the type and size right so that I can
				// calculate the offset right.

				// evaluate the operand as an address
				Expression structBasePointer = asAddressInternal(middle, result,
				    possibleAddresses);
				// add field offset to the base pointer.
				Type type = middle.getType();
				String fieldName = ((StructAccessOperator) op).getField();
				// find the offset of the field inside the struct
				if (type instanceof StructType) {
					int offset = 0;
					StructType structType = (StructType) type;
					for (int i = 0; i < structType.getFields().size(); i++) {
						if (fieldName.equals(structType.getFields().get(i))) {
							break;
						}
						offset += structType.getFieldTypes().get(i).size();
					}
					Expression address = new BinaryOpExpression(new PlusOperator(),
					    IntConstant.valueOf(offset), structBasePointer);

					ArrayList<Expression> newPossibleAddress = new ArrayList<Expression>();
					for (Expression addressExpression : possibleAddresses) {
						newPossibleAddress.add(new BinaryOpExpression(new PlusOperator(),
						    IntConstant.valueOf(offset), addressExpression));
					}
					possibleAddresses.clear();
					possibleAddresses.addAll(newPossibleAddress);
					return address;
				} else {
					throw new RuntimeException("Try to access field on non-struct type.");
				}
			}
		}
		throw new RuntimeException("I don't know how to interpret " + expression
		    + " as an address.");
	}

	private static void evaluatePossibleValues(Expression expression,
	    Set<Expression> possibleValues, int depth) {
		IntConstant c = IntConstant.toIntConstant(expression);
		if (c != null) {
			possibleValues.add(c);
			return;
		}
		if (expression instanceof LvalExpression) {
			if (expression.getType() instanceof RestrictedIntType) {
				// investigate in the value range
				int bits = IntType.getBits((IntType) expression.getType());
				// TODO we can allow more possibilities here.
				// to support things like *(p+x) where x is a fair small number.
				if (bits < 2) {
					IntConstantInterval interval = ((RestrictedIntType) expression
					    .getType()).getInterval();
					int lower = interval.lower.toInt();
					int upper = interval.upper.toInt();
					for (int i = lower; i <= upper; i++) {
						possibleValues.add(IntConstant.valueOf(i));
					}
					return;
				}
			}
			// try to follow assignment's right hand side
			// we need to limit the depth here.
			if (depth < 10) {
				StatementWithOutputLine assignment = ((LvalExpression) expression)
				    .getAssigningStatement();
				if (assignment instanceof AssignmentStatement) {
					evaluatePossibleValues(((AssignmentStatement) assignment).getRHS(),
					    possibleValues, depth + 1);
				}
			}
		} else if (expression instanceof UnaryOpExpression) {
			evaluatePossibleValues(((UnaryOpExpression) expression).getMiddle(),
			    possibleValues, depth);
		} else if (expression instanceof BinaryOpExpression) {
			HashSet<Expression> possibleAddressLeft = new HashSet<Expression>();
			HashSet<Expression> possibleAddressRight = new HashSet<Expression>();
			evaluatePossibleValues(((BinaryOpExpression) expression).getLeft(),
			    possibleAddressLeft, depth);
			evaluatePossibleValues(((BinaryOpExpression) expression).getRight(),
			    possibleAddressRight, depth);
			for (Expression left : possibleAddressLeft) {
				for (Expression right : possibleAddressRight) {
					possibleValues.add(new BinaryOpExpression(
					    ((BinaryOpExpression) expression).getOperator(), left, right));
				}
			}
		} else if (expression instanceof PolynomialExpression) {
			PolynomialExpression pe = (PolynomialExpression) expression;
			Expression oe = pe.toOperationExpression();
			evaluatePossibleValues(oe, possibleValues, depth);
		} else {
			throw new RuntimeException(
			    "I don't know how to evaluate the value of the pointer.");
		}
	}

	@Override
	public String toString() {
		return "*()";
	}
}
