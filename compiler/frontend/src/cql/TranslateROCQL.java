import java.util.List;
import java.util.Map.Entry;

import org.apache.cassandra.cql.CQLStatement;
import org.apache.cassandra.cql.CreateColumnFamilyStatement;
import org.apache.cassandra.cql.CreateIndexStatement;
import org.apache.cassandra.cql.QueryProcessor;
import org.apache.cassandra.cql.Relation;
import org.apache.cassandra.cql.RelationType;
import org.apache.cassandra.cql.SelectStatement;
import org.apache.cassandra.cql.Term;
import org.apache.cassandra.cql.UpdateStatement;
import org.apache.commons.lang.NotImplementedException;

public class TranslateROCQL {
	public static void translateCQL(String cql, int size, String[] output)
	    throws Exception {
		CQLStatement statement = QueryProcessor.getStatement(cql);
		switch (statement.type) {
		case SELECT:
			translateSelect((SelectStatement) statement.statement, size, output);
			return;
		case INSERT:
			translateInsert((UpdateStatement) statement.statement, size, output);
			return;
		case UPDATE:
			translateUpdate((UpdateStatement) statement.statement, size, output);
			return;
		case CREATE_COLUMNFAMILY:
			translateCreateColumnFamily(
			    (CreateColumnFamilyStatement) statement.statement, size, output);
			return;
		case CREATE_INDEX:
			translateCreateIndex((CreateIndexStatement) statement.statement, size,
			    output);
			return;
		default:
			// NOT supported yet.
			throw new NotImplementedException();
		}
	}

	private static void translateCreateIndex(CreateIndexStatement s, int size,
	    String[] output) {
		Metadata md = KeySpaceMetadata.getMetadata(s.getColumnFamily());
		ColumnMetadata cm = md.getColumn(s.getColumnName().getText());
		cm.indexFlag = 1;
		KeySpaceMetadata.addMetadata(s.getColumnFamily(), md);
	}

	private static void translateCreateColumnFamily(
	    CreateColumnFamilyStatement s, int size, String[] output) {
		Metadata metadata = new Metadata(s.getName());
		ColumnMetadata cm = new ColumnMetadata("KEY", s.getKeyType(), 1);
		metadata.addColumn(cm);

		for (Entry<Term, String> column : s.getColumns().entrySet()) {
			cm = new ColumnMetadata(column.getKey().getText(), column.getValue(), 0);
			metadata.addColumn(cm);
		}
		KeySpaceMetadata.addMetadata(s.getName(), metadata);

		exoCreateDB(s.getName(), size);
	}

	/**
	 * transform a given CQL select statement into put/get. Currently USING clause
	 * will be ignored. using wildchar is not supported yet. There is only one
	 * default keyspace. assume that an index for every column exist. key range
	 * not supported yet. multiple where clause not supported yet.
	 * 
	 * @param output
	 * 
	 * @param s
	 * @param size
	 * @return
	 */
	private static void translateSelect(SelectStatement s, int size,
	    String[] output) {
		Metadata metadata = KeySpaceMetadata.getMetadata(s.getColumnFamily());
		List<Term> columns = s.getColumnNames();
		// Set<Term> keys = s.getKeys();
		List<Relation> relations = s.getRelations();
		String cfName = s.getColumnFamily();

		if (relations.size() == 0) {
			throw new NotImplementedException();
		}
		if (relations.size() > 1) {
			if (relations.size() > 2) {
				throw new NotImplementedException();
			}
			Relation firstRelation = relations.get(0);
			Relation secondRelation = relations.get(1);
			if (firstRelation.getEntity().getText()
			    .equals(secondRelation.getEntity().getText())) {
				String entity = firstRelation.getEntity().getText();
				if ((firstRelation.operator() != RelationType.GTE && firstRelation
				    .operator() != RelationType.GT)
				    || (secondRelation.operator() != RelationType.LTE && secondRelation
				        .operator() != RelationType.LT)) {
					throw new NotImplementedException();
				} else {
					String lowerBound = firstRelation.getValue().getText();
					String upperBound = secondRelation.getValue().getText();

					ColumnMetadata em = metadata.getColumn(entity);
					if (em.indexFlag == 0) {
						throw new NotImplementedException(
						    "Query on non indexed column not supported.");
					}
					PrintAPI.assignment("temp" + entity, "" + lowerBound, em.type, true);
					// lower bound
					switch (firstRelation.operator()) {
					case GT: {
						printBinarySearch(em.offset + "", size, "temp" + entity + " + 1");
						System.out.printf("%slowerBound = right + 1;\n",
						    PrintAPI.getIndent());
						// System.out.printf("%supperBound = %d;\n", PrintAPI.getIndent(),
						// em.offset + size - 1);
						break;
					}
					case GTE: {
						printBinarySearch(em.offset + "", size, "temp" + entity);
						System.out.printf("%slowerBound = right + 1;\n",
						    PrintAPI.getIndent());
						// System.out.printf("%supperBound = %d;\n", PrintAPI.getIndent(),
						// em.offset + size - 1);
						break;
					}
					default:
						break;
					}
					PrintAPI.assignment("temp" + entity, "" + upperBound, em.type, true);
					// uppper bound
					switch (secondRelation.operator()) {
					case LT: {
						// System.out.printf("%slowerBound = %d;\n", PrintAPI.getIndent(),
						// em.offset);
						printBinarySearch(em.offset + "", size, "temp" + entity);
						System.out.printf("%supperBound = right;\n", PrintAPI.getIndent());
						break;
					}
					case LTE: {
						// System.out.printf("%slowerBound = %d;\n", PrintAPI.getIndent(),
						// em.offset);
						printBinarySearch(em.offset + "", size, "temp" + entity + " + 1");
						System.out.printf("%supperBound = right;\n", PrintAPI.getIndent());
						break;
					}
					default:
						break;
					}
				}
			} else {
				throw new NotImplementedException();
			}
		} else {
			Relation relation = relations.get(0);
			String value = relation.getValue().getText();
			String entity = relation.getEntity().getText();
			ColumnMetadata em = metadata.getColumn(entity);
			if (em.indexFlag == 0) {
				throw new NotImplementedException(
				    "Query on non indexed column not supported.");
			}
			PrintAPI.assignment("temp" + entity, "" + value, em.type, true);

			// just call range query to get the results.
			assert (output.length == columns.size());
			switch (relation.operator()) {
			case EQ: {
				// perform a binary search on that index.
				// I use the offset as index offset
				printBinarySearch(em.offset + "", size, "temp" + entity);
				System.out.printf("%slowerBound = right + 1;\n", PrintAPI.getIndent());
				printBinarySearch(em.offset + "", size, "temp" + entity + " + 1");
				System.out.printf("%supperBound = right;\n", PrintAPI.getIndent());
				break;
			}
			case LT: {
				System.out.printf("%slowerBound = %d;\n", PrintAPI.getIndent(),
				    em.offset);
				printBinarySearch(em.offset + "", size, "temp" + entity);
				System.out.printf("%supperBound = right;\n", PrintAPI.getIndent());
				break;
			}
			case LTE: {
				System.out.printf("%slowerBound = %d;\n", PrintAPI.getIndent(),
				    em.offset);
				printBinarySearch(em.offset + "", size, "temp" + entity + " + 1");
				System.out.printf("%supperBound = right;\n", PrintAPI.getIndent());
				break;
			}
			case GT: {
				printBinarySearch(em.offset + "", size, "temp" + entity + " + 1");
				System.out.printf("%slowerBound = right + 1;\n", PrintAPI.getIndent());
				System.out.printf("%supperBound = %d;\n", PrintAPI.getIndent(),
				    em.offset + size - 1);
				break;
			}
			case GTE: {
				printBinarySearch(em.offset + "", size, "temp" + entity);
				System.out.printf("%slowerBound = right + 1;\n", PrintAPI.getIndent());
				System.out.printf("%supperBound = %d;\n", PrintAPI.getIndent(),
				    em.offset + size - 1);
				break;
			}
			default:
				throw new NotImplementedException();
			}
		}
		System.out.printf("%sfor (i = 0; i < %d; i++) {\n", PrintAPI.getIndent(),
		    s.getNumRecords());
		PrintAPI.increaseIndent();
		{
			System.out.printf("%sif (i + lowerBound < upperBound) {\n",
			    PrintAPI.getIndent());
			PrintAPI.increaseIndent();
			{
				PrintAPI.ramGet("tempHash", "i + lowerBound + " + size);
				PrintAPI.hashGet("temp" + cfName, "tempHash");
				if (s.isWildcard()) {
					for (ColumnMetadata cm : metadata.getColumns()) {
						String columnName = cm.name;
						PrintAPI.assignment(output[0] + "[i]." + columnName, "temp"
						    + cfName + "." + columnName, cm.type, false);
					}
				} else {
					for (int j = 0; j < columns.size(); j++) {
						String columnName = columns.get(j).getText();
						ColumnMetadata cm = metadata.getColumn(columnName);
						PrintAPI.assignment(output[0] + "[i]." + columnName, "temp"
						    + cfName + "." + columnName, cm.type, false);
					}
				}
				// PrintAPI.assignment("output[i]", "temp" + cfName, cfName + "_t",
				// false);
			}
			PrintAPI.decreaseIndent();
			System.out.printf("%s} else {\n", PrintAPI.getIndent());

			PrintAPI.increaseIndent();
			{
				if (s.isWildcard()) {
					for (ColumnMetadata cm : metadata.getColumns()) {
						String columnName = cm.name;
						PrintAPI.assignment(output[0] + "[i]." + columnName, null, cm.type,
						    true);
					}
				} else {
					for (int j = 0; j < columns.size(); j++) {
						String columnName = columns.get(j).getText();
						ColumnMetadata cm = metadata.getColumn(columnName);
						PrintAPI.assignment(output[0] + "[i]." + columnName, null, cm.type,
						    true);
					}
				}
				// PrintAPI.assignment("output[i]", "null" + cfName, cfName + "_t",
				// false);
			}
			PrintAPI.decreaseIndent();
			System.out.printf("%s}\n", PrintAPI.getIndent());
		}
		PrintAPI.decreaseIndent();
		System.out.printf("%s}\n", PrintAPI.getIndent());
	}

	private static void printBinarySearch(String left, int size, String value) {
		// computation.
		// int right = left + size - 1;
		System.out.printf("%sleft = %s;\n", PrintAPI.getIndent(), left);
		System.out.printf("%sright = %s;\n", PrintAPI.getIndent(), left + " + "
		    + size + "- 1");

		System.out.printf("%sfor(i = 0; i < logSIZE; i++) {\n",
		    PrintAPI.getIndent());
		PrintAPI.increaseIndent();
		{
			System.out.printf("%smid = (left + right) >> 1;\n", PrintAPI.getIndent());
			PrintAPI.ramGet("value", "mid");
			System.out.printf("%sif (value < %s) {\n", PrintAPI.getIndent(), value);
			PrintAPI.increaseIndent();
			{
				System.out.printf("%sleft = mid + 1;\n", PrintAPI.getIndent());
			}
			PrintAPI.decreaseIndent();
			System.out.printf("%s} else {\n", PrintAPI.getIndent());
			PrintAPI.increaseIndent();
			{
				System.out.printf("%sright = mid - 1;\n", PrintAPI.getIndent());
			}
			PrintAPI.decreaseIndent();
			System.out.printf("%s}\n", PrintAPI.getIndent());
		}
		PrintAPI.decreaseIndent();
		// System.out.printf("  }\n");
		System.out.printf("%s}\n", PrintAPI.getIndent());
		// return right;
	}

	private static void translateInsert(UpdateStatement s, int size,
	    String[] output) {
		List<Term> columns = s.getColumnNames();
		List<Term> values = s.getColumnValues();
		for (int i = 0; i < columns.size(); i++) {
			System.out.println(columns.get(i) + ":" + values.get(i));
		}
		// return s.toString();
	}

	private static void translateUpdate(UpdateStatement s, int size,
	    String[] output) {
		// return s.toString();
	}

	//
	// private static String[] states = { "AK", "AL", "AR", "AZ", "CA", "CO",
	// "CT",
	// "DE", "FL", "GA", "HI", "IA", "ID", "IL", "IN", "KS", "KY", "LA", "MA",
	// "MD", "ME", "MI", "MN", "MO", "MS", "MT", "NC", "ND", "NE", "NH", "NJ",
	// "NM", "NV", "NY", "OH", "OK", "OR", "PA", "RI", "SC", "SD", "TN", "TX",
	// "UT", "VA", "WA", "WI", "WV", "WY", };
	//
	// private static String[] majors = { "CS", "ECE", "MAT", "STA", "CHE", "BUS",
	// "LIB", "ME", "CIV" };

	public static void exoCreateDB(String cfName, int dbSize) {
		// TODO change this.
		// Assume that the index is on some columns.
		// int id;
		// String fname;
		// String lname;
		// int age;
		// String major;
		// String state;
		// int phoneNum;
		// int class_;
		// int credits;
		// int avg;
		// Random random = new Random(System.nanoTime());
		// for (int i = 0; i < dbSize; i++) {
		// id = i;
		// fname = "firstname";
		// lname = "lastname";
		// // TODO age should be sorted.
		// age = 24 + random.nextInt(100000) / 100000;
		// major = majors[random.nextInt(majors.length)];
		// state = states[random.nextInt(states.length)];
		// phoneNum = 10000000 + random.nextInt(89999999);
		// class_ = random.nextInt(5) + 2009;
		// credits = (class_ - 2008) * 15 + random.nextInt(5);
		// avg = (int) (random.nextGaussian() * 40 + 60);
		// System.out.printf("tempStudent.KEY = %d;\n", id);
		// System.out.printf("strcpy(tempStudent.FName, \"%s\");\n", fname);
		// System.out.printf("strcpy(tempStudent.LName, \"%s\");\n", lname);
		// System.out.printf("tempStudent.Age = %d;\n", age);
		// System.out.printf("strcpy(tempStudent.Major, \"%s\");\n", major);
		// System.out.printf("strcpy(tempStudent.State, \"%s\");\n", state);
		// System.out.printf("tempStudent.PhoneNum = %d;\n", phoneNum);
		// System.out.printf("tempStudent.Class = %d;\n", class_);
		// System.out.printf("tempStudent.Credits = %d;\n", credits);
		// System.out.printf("tempStudent.Average = %d;\n", avg);
		// }
	}
}