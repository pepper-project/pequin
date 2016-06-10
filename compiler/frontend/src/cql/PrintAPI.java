import org.apache.cassandra.cql.RelationType;

public class PrintAPI {
	private static int indent = 4;

	public static String getIndent() {
		StringBuffer outputBuffer = new StringBuffer(indent);
		for (int i = 0; i < indent; i++) {
			outputBuffer.append(" ");
		}
		return outputBuffer.toString();
	}

	public static void increaseIndent() {
		indent += 4;
	}

	public static void decreaseIndent() {
		indent -= 4;
	}

	public static void ramPut(String addr, String data) {
		System.out.printf("%sramput(%s, &(%s));\n", getIndent(), addr, data);
	}

	public static void ramGet(String var, String addr) {
		System.out.printf("%sramget(&(%s), %s);\n", getIndent(), var, addr);
	}

	public static void assignment(String lhs, String rhs, String type,
	    boolean literal) {
		if (type.equals("ascii")) {
			if (rhs == null) {
				rhs = "\"\"";
			} else if (literal) {
				rhs = "\"" + rhs + "\"";
			}
			System.out.printf("%sstrcpy(%s, %s);\n", getIndent(), lhs, rhs);
		} else {
			if (rhs == null) {
				rhs = "0";
			}
			System.out.printf("%s%s = %s;\n", getIndent(), lhs, rhs);
		}
	}

	public static void hashPut(String key, String value) {
		System.out.printf("%shashput(&(%s), &(%s));\n", getIndent(), key, value);
	}

	public static void hashGet(String var, String key) {
		System.out.printf("%shashget(&(%s), &(%s));\n", getIndent(), var, key);
	}

	public static void treeInsert(String tree, String key, String value) {
		System.out.printf("%stree_insert(&(%s), (%s), (%s));\n", getIndent(), tree,
		    key, value);
	}

	public static void treeUpdate(String tree, String key, String oldValue,
	    String newValue) {
		System.out.printf("%stree_update(&(%s), (%s), (%s), (%s));\n", getIndent(),
		    tree, key, oldValue, newValue);
	}

	public static void treeUpdateNoBalance(String tree, String key,
	    String oldValue, String newValue, String maxDepth, String treePath) {
		System.out.printf(
		    "%stree_update_no_balance(&(%s), (%s), (%s), (%s), &(%s), &(%s));\n",
		    getIndent(), tree, key, oldValue, newValue, maxDepth, treePath);
	}

	public static void treeBalance(String tree, String maxDepth, String treePath) {
		System.out.printf("%stree_balance(&(%s), (%s), (%s));\n", getIndent(),
		    tree, maxDepth, treePath);
	}

	public static void treeRemoveValue(String tree, String key, String value) {
		System.out.printf("%stree_remove_value(&(%s), (%s), (%s));\n", getIndent(),
		    tree, key, value);

	}

	public static void treeRemove(String tree, String key) {
		System.out.printf("%stree_remove(&(%s), (%s));\n", getIndent(), tree, key);

	}

	// public static void treeFind(String var, String tree, String key) {
	// System.out.printf("%stree_find_eq(&(%s), (%s), &(%s));\n", getIndent(),
	// tree, key, var);
	// }

	public static void treeFind(String var, String tree, String key,
	    RelationType op, int limit) {
		switch (op) {
		case EQ:
			System.out.printf("%stree_find_eq(&(%s), (%s), &(%s));\n", getIndent(),
			    tree, key, var);
			break;
		case GT:
			System.out.printf("%stree_find_gt(&(%s), (%s), FALSE, &(%s));\n",
			    getIndent(), tree, key, var);
			break;
		case GTE:
			System.out.printf("%stree_find_gt(&(%s), (%s), TRUE, &(%s));\n",
			    getIndent(), tree, key, var);
			break;
		case LT:
			System.out.printf("%stree_find_lt(&(%s), (%s), FALSE, &(%s));\n",
			    getIndent(), tree, key, var);
			break;
		case LTE:
			System.out.printf("%stree_find_lt(&(%s), (%s), TRUE, &(%s));\n",
			    getIndent(), tree, key, var);
			break;
		}
	}

	public static void treeFind(String var, String tree, String lowerBound,
	    String lowerBoundInclusive, String upperBound,
	    String upperBoundInclusive, int limit) {
		System.out.printf(
		    "%stree_find_range(&(%s), (%s), (%s), (%s), (%s), &(%s));\n",
		    getIndent(), tree, lowerBound, lowerBoundInclusive, upperBound,
		    upperBoundInclusive, var);
	}
}
