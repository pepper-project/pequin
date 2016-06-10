import java.util.HashMap;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.apache.cassandra.cql.RelationType;

public class API {
	// implemented for testing purpose.
	private static final int SIZE = 1024;
	private static String[] ram = new String[SIZE];

//	private static HashMap<String, HashMap<String, Integer>> allHashMap = new HashMap<String, HashMap<String, Integer>>();
	private static HashMap<String, TreeMap<String, Integer>> allTreeMap = new HashMap<String, TreeMap<String, Integer>>();

	static {
		for (int i = 0; i < SIZE; i++) {
//			ram[i] = "0";
		}
	}

	public static void ramPut(int addr, String data) {
		System.out.printf("DEBUG: WRITE ram[%d] = %s\n", addr, data);
		ram[addr] = data;
	}

	public static String ramGet(int addr) {
		System.out.printf("DEBUG: READ ram[%d]: %s\n", addr, ram[addr]);
		return ram[addr];
	}

	public static void treeInsert(String tree, String key, Integer value) {
		if (!allTreeMap.containsKey(tree)) {
			allTreeMap.put(tree, new TreeMap<String, Integer>());
		}
		allTreeMap.get(tree).put(key, value);
	}

	public static void treeDelete(String tree, String key) {
		if (!allTreeMap.containsKey(tree)) {
			allTreeMap.put(tree, new TreeMap<String, Integer>());
		}
		allTreeMap.get(tree).remove(key);
	}

	public static int treeFind(String tree, String key) {
		if (!allTreeMap.containsKey(tree)) {
			allTreeMap.put(tree, new TreeMap<String, Integer>());
		}
		if (allTreeMap.get(tree).containsKey(key)) {
			return allTreeMap.get(tree).get(key);
		}
		return -1;
	}

	public static int[] treeRange(String tree, String key, RelationType op,
			int limit) {
		if (!allTreeMap.containsKey(tree)) {
			allTreeMap.put(tree, new TreeMap<String, Integer>());
		}
		int[] result = new int[limit];
		NavigableMap<String, Integer> range = null;
		switch (op) {
		case EQ:
			range = allTreeMap.get(tree).subMap(key, true, key, true);
			break;
		case GT:
			range = allTreeMap.get(tree).tailMap(key, false);
			break;
		case GTE:
			range = allTreeMap.get(tree).tailMap(key, true);
			break;
		case LT:
			range = allTreeMap.get(tree).headMap(key, false);
			break;
		case LTE:
			range = allTreeMap.get(tree).headMap(key, true);
			break;
		}
		Iterator<Integer> iter = range.values().iterator();
		for (int i = 0; i < limit; i++) {
			if (iter.hasNext()) {
				result[i] = iter.next();
			} else {
				result[i] = -1;
			}
		}
		return result;
	}
}
