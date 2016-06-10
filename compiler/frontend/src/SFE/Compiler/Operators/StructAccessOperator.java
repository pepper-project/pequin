package SFE.Compiler.Operators;

import java.util.HashSet;

import SFE.Compiler.AnyType;
import SFE.Compiler.AssignmentStatement;
import SFE.Compiler.BlockStatement;
import SFE.Compiler.CacheManager;
import SFE.Compiler.Expression;
import SFE.Compiler.Function;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.RamGetEnhancedStatement;
import SFE.Compiler.SLPTReduction;
import SFE.Compiler.Statement;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.StructType;
import SFE.Compiler.Type;
import SFE.Compiler.UnaryOpExpression;
import SFE.Compiler.UnaryOperator;

public class StructAccessOperator extends Operator implements UnaryOperator,
    SLPTReduction {
	private String field;

	public StructAccessOperator(String field) {
		this.field = field;
	}

	public String toString() {
		return "struct access " + field;
	}

	public Expression fieldEltAt(Expression expression, int i) {
		throw new RuntimeException("Not implemented");
	}

	public int arity() {
		return 1;
	}

	public String getField() {
		return field;
	}

	public int priority() {
		throw new RuntimeException("Not implemented");
	}

	public Type getType(Object obj) {
		// Not determinate type -> should return AnyType.
		if (obj instanceof UnaryOpExpression) {
			UnaryOpExpression uoe = (UnaryOpExpression) obj;
			if (uoe.metaType != null) {
				return uoe.metaType;
			}
		}
		return new AnyType();
	}

	public Expression inlineOp(StatementBuffer assignments, Expression... args) {
		return resolve(args);
	}

	public LvalExpression resolve(Expression... args) {
		LvalExpression a = LvalExpression.toLvalExpression(args[0]);
		if (a != null) {

			// Check bounds.
			if (!(a.getType() instanceof StructType)) {
				throw new RuntimeException(
				    "Cannot perform struct field access on value " + a + " with type "
				        + a.getType());
			}

			boolean hasField = ((StructType) a.getType()).getFields().contains(field);
			if (!hasField) {
				throw new RuntimeException("Cannot access field " + field
				    + " on variable " + a + " with type " + a.getType());
			}

			return Function.getVars().getVar(a.getName() + "." + field)
			    .changeReference(Function.getVars());
		}

		return null;
	}

	@Override
	public Statement toSLPTCircuit(Object obj) {
		// if code reaches this pointer, it must be compile time non-resolvable
		// struct accesses.
		// resolve struct access for dynamic pointers.
		BlockStatement result = new BlockStatement();

		AssignmentStatement as = (AssignmentStatement) obj;
		LvalExpression lhs = as.getLHS();
		UnaryOpExpression rhs = (UnaryOpExpression) as.getRHS();

		lhs.setType(rhs.getType());

//		LvalExpression base = PointerAccessOperator.getBaseAddress(rhs);
		HashSet<Integer> possibleAddresses = new HashSet<Integer>();
		Expression address = PointerAccessOperator.asAddress(rhs, result, possibleAddresses);

//		Statement cacheManagementStatement = CacheManager.invalidateCache(base);
//		cacheManagementStatement.toSLPTCircuit(null);
//		result.addStatement(cacheManagementStatement);
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
}
