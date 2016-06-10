import java.util.List;
import java.util.Map.Entry;

import org.apache.cassandra.cql.CQLStatement;
import org.apache.cassandra.cql.CreateColumnFamilyStatement;
import org.apache.cassandra.cql.CreateIndexStatement;
import org.apache.cassandra.cql.DropIndexStatement;
import org.apache.cassandra.cql.QueryProcessor;
import org.apache.cassandra.cql.Relation;
import org.apache.cassandra.cql.SelectStatement;
import org.apache.cassandra.cql.Term;
import org.apache.cassandra.cql.UpdateStatement;
import org.apache.commons.lang.NotImplementedException;

public class TranslateCQL {
	// NEED to revisit this because the interface zaatar provides are subject to
	// change.
	public static final int DB_SIZE_OFFSET = 0;
	public static final int DB_NUM_OF_ROW_OFFSET = 1;
	public static final int DB_DATA_OFFSET = 2;

	/**
	 * The source code file must have the following variables declared:
	 * 
	 * int temp, tempID, tempKey, tempColumnValue;
	 * 
	 * int i, j;
	 * 
	 * int keyIndex, nextRowID, numberOfRows;
	 * 
	 * int columnOffset, columnValue, columnOldValue;
	 * 
	 * int result[LIMIT];
	 * 
	 * binary_tree_t* <ColumnName>_index = 0;
	 * 
	 * The user should make sure that output is enough to hold the result of
	 * select statement. The amount of rows returned by select is by default 3
	 * rows. To get more rows, use LIMIT <number> clause inside select statement.
	 * In either case, the size of the array to hold return result should be at
	 * least <number of rows>. Each output corresponds to one column of the select
	 * statement. For example, the following statement will store the first column
	 * name in output->output[0], and the second column age in output->output[1].
	 * output->output[0] and output->output[1] should have at least 3 entries to
	 * store the results.
	 * 
	 * CQL("SELECT name,age FROM S WHERE age < 8", 16, output->output[0],
	 * output->output[1]);
	 * 
	 * @param cql
	 * @param size
	 * @param output
	 * @throws Exception
	 */
	public static void tranlsateCQL(String cql, int size, String... output)
	    throws Exception {
		CQLStatement statement = QueryProcessor.getStatement(cql);
		System.out.printf("%s//%s\n", PrintAPI.getIndent(), cql);
		System.out.printf("#ifdef DEBUG\n");
		System.out.printf("%sprintf(\"%s\\n\");\n", PrintAPI.getIndent(), cql);
		System.out.printf("#endif\n");

		switch (statement.type) {
		case SELECT:
			translateSelect((SelectStatement) statement.statement, size, output);
			break;
		case INSERT:
			translateInsert((UpdateStatement) statement.statement, size, output);
			break;
		case UPDATE:
			translateUpdate((UpdateStatement) statement.statement, size, output);
			break;
		case CREATE_COLUMNFAMILY:
			translateCreateColumnFamily(
			    (CreateColumnFamilyStatement) statement.statement, size, output);
			break;
		case CREATE_INDEX:
			translateCreateIndex((CreateIndexStatement) statement.statement, size,
			    output);
			break;
		case DROP_INDEX:
			translateDropIndex((DropIndexStatement) statement.statement, size, output);
			break;
		case DROP_COLUMNFAMILY:
			translateDropColumnFamily((String) statement.statement, size, output);
			break;
		default:
			// NOT supported yet.
			throw new NotImplementedException();
		}
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
	    String... output) {
		if (s.isWildcard()) {
			throw new NotImplementedException();
		}
		Metadata metadata = KeySpaceMetadata.getMetadata(s.getColumnFamily());
		List<Term> columns = s.getColumnNames();
		// Set<Term> keys = s.getKeys();
		List<Relation> relations = s.getRelations();
		String cfName = s.getColumnFamily();

		if (relations.size() != 1) {
			throw new NotImplementedException(
			    "only support exactly 1 where clause queries");
		}
		for (Relation relation : relations) {
			String value = relation.getValue().getText();
			String entity = relation.getEntity().getText();
			ColumnMetadata em = metadata.getColumn(entity);

			PrintAPI.assignment("temp" + entity, value, em.type, true);
			PrintAPI.treeFind("result", entity + "_index", "temp" + entity,
			    relation.operator(), s.getNumRecords());

			// just call range query to get the results.
			assert (output.length == columns.size());

			System.out.printf("%sfor (i = 0; i < %d; i++) {\n", PrintAPI.getIndent(),
			    s.getNumRecords());
			PrintAPI.increaseIndent();
			{
				System.out.printf("%sif (i < result.num_results) {\n",
				    PrintAPI.getIndent());
				PrintAPI.increaseIndent();
				{
					PrintAPI.ramGet("temp" + cfName,
					    "DB_DATA_OFFSET + result.results[i].value");
					for (int j = 0; j < columns.size(); j++) {
						String columnName = columns.get(j).getText();
						ColumnMetadata cm = metadata.getColumn(columnName);
						PrintAPI.assignment(output[j] + "[i]", "temp" + cfName + "."
						    + columnName, cm.type, false);
					}
				}
				PrintAPI.decreaseIndent();
				System.out.printf("%s} else {\n", PrintAPI.getIndent());
				PrintAPI.increaseIndent();
				for (int j = 0; j < columns.size(); j++) {
					String columnName = columns.get(j).getText();
					ColumnMetadata cm = metadata.getColumn(columnName);
					PrintAPI.assignment(output[j] + "[i]", null, cm.type, true);
				}
			}
			PrintAPI.decreaseIndent();
			System.out.printf("%s}\n", PrintAPI.getIndent());
			PrintAPI.decreaseIndent();
			System.out.printf("%s}\n", PrintAPI.getIndent());

			System.out.printf("#ifdef DEBUG\n");
			System.out.printf("%sfor(i = 0; i < result.num_results; i++) {\n",
			    PrintAPI.getIndent());
			PrintAPI.increaseIndent();
			{
				for (int j = 0; j < columns.size(); j++) {

					String columnName = columns.get(j).getText();
					ColumnMetadata cm = metadata.getColumn(columnName);
					String placeHolder = "";
					// the place holder should depend on the type of the column
					if (cm.type.equals("ascii")) {
						placeHolder = "%s";
					} else {
						placeHolder = "%d";
					}
					System.out.printf("%sprintf(\"%s: %s \", %s[i]);\n",
					    PrintAPI.getIndent(), columns.get(j).getText(), placeHolder,
					    output[j]);
				}
				System.out.printf("%sprintf(\"\\n\");\n", PrintAPI.getIndent());
			}
			PrintAPI.decreaseIndent();
			System.out.printf("%s}\n", PrintAPI.getIndent());
			System.out.printf("#endif\n");
			break;
		}
	}

	private static void translateInsert(UpdateStatement s, int size,
	    String... output) {
		Metadata metadata = KeySpaceMetadata.getMetadata(s.getColumnFamily());
		List<Term> columns = s.getColumnNames();
		List<Term> values = s.getColumnValues();
		// get value of key
		String key = s.getKeys().get(0).getText();
		String cfName = s.getColumnFamily();

		{
			{

				// otherwise, find the next available slot
				PrintAPI.ramGet("nextRowID", "DB_NUM_OF_ROW_OFFSET");
				// update number of rows.
				System.out.printf("%stempRowID = nextRowID + 1;\n",
				    PrintAPI.getIndent());
				PrintAPI.ramPut("DB_NUM_OF_ROW_OFFSET", "tempRowID");
				// the column id for primary key is always 0.
				// ColumnMetadata cm = metadata.getColumn("KEY");
				ColumnMetadata cm = metadata.getColumn("KEY");
				PrintAPI.assignment("temp" + cfName + ".KEY", key, cm.type, true);

				// PrintAPI.ramPut("columnOffset", "tempKEY");
				// PrintAPI.treeInsert("KEY_index", "temp" + cfName + ".KEY",
				// "nextRowID");
			}
			// calculate the offset and insert into it
			for (int i = 0; i < columns.size(); i++) {
				String columnName = columns.get(i).getText();
				String columnValue = values.get(i).getText();
				ColumnMetadata cm = metadata.getColumn(columnName);

				// System.out.printf("%scolumnOffset = %d + nextRowID * %d;\n",
				// PrintAPI.getIndent(), cm.offset, cm.size);
				PrintAPI.assignment("temp" + cfName + "." + columnName, columnValue,
				    cm.type, true);
				// PrintAPI.ramPut("columnOffset", "temp" + columnName);

				// update index
				if (cm.indexFlag == 1) {
					PrintAPI.treeInsert(columnName + "_index", "temp" + cfName + "."
					    + columnName, "nextRowID");
				}
			}
			System.out.printf("%srowOffset = DB_DATA_OFFSET + nextRowID;\n",
			    PrintAPI.getIndent());
			PrintAPI.ramPut("rowOffset", "temp" + cfName);
			// PrintAPI.decreaseIndent();
			// System.out.printf("%s}\n", PrintAPI.getIndent());
		}
	}

	private static void translateUpdate(UpdateStatement s, int size,
	    String... output) {
		// TODO similar to insert
		// return s.toString();
	}

	private static void translateCreateColumnFamily(
	    CreateColumnFamilyStatement s, int size, String... output) {
		// System.out.printf("name: %s\n", s.getName());
		Metadata metadata = new Metadata(s.getName());

		System.out.printf("%stempInt = %d;\n", PrintAPI.getIndent(), size);
		PrintAPI.ramPut("DB_SIZE_OFFSET", "tempInt");
		System.out.printf("%stempInt = 0;\n", PrintAPI.getIndent());
		PrintAPI.ramPut("DB_NUM_OF_ROW_OFFSET", "tempInt");

		// the first column should be the primary key.
		// column name: KEY
		// int currentColumnOffset = DB_DATA_OFFSET;
		// int columnSize = 1;

		ColumnMetadata cm = new ColumnMetadata("KEY", s.getKeyType(), 1);
		metadata.addColumn(cm);

		for (Entry<Term, String> column : s.getColumns().entrySet()) {
			// System.out.printf("column name: %s type: %s\n", column.getKey(),
			// column.getValue());

			cm = new ColumnMetadata(column.getKey().getText(), column.getValue(), 0);
			metadata.addColumn(cm);

		}
		KeySpaceMetadata.addMetadata(s.getName(), metadata);
	}

	private static void translateCreateIndex(CreateIndexStatement s, int size,
	    String... output) {
		// get column meta data
		Metadata metadata = KeySpaceMetadata.getMetadata(s.getColumnFamily());
		String columnName = s.getColumnName().getText();
		String indexName = columnName + "_index";
		String cfName = s.getColumnFamily();

		ColumnMetadata cm = metadata.getColumn(columnName);

		// insert them into the tree.
		PrintAPI.ramGet("numberOfRows", "DB_NUM_OF_ROW_OFFSET");
		// TODO this requires dynamic looping.
		System.out.printf("%sfor (i = 0; i < %d; i++) {\n", PrintAPI.getIndent(),
		    size);
		PrintAPI.increaseIndent();
		{
			System.out.printf("%sif (i < numberOfRows) {\n", PrintAPI.getIndent());
			PrintAPI.increaseIndent();
			{
				PrintAPI.ramGet("temp" + cfName, "DB_DATA_OFFSET + i");
				PrintAPI.treeInsert(indexName, "temp" + cfName + "." + columnName, "i");
			}
			PrintAPI.decreaseIndent();
			System.out.printf("%s}\n", PrintAPI.getIndent());
		}
		PrintAPI.decreaseIndent();
		System.out.printf("%s}\n", PrintAPI.getIndent());
		cm.indexFlag = 1;
	}

	private static void translateDropIndex(DropIndexStatement statement,
	    int size, String... output) {
		// TODO Auto-generated method stub
		throw new NotImplementedException();
	}

	private static void translateDropColumnFamily(String cf, int size,
	    String... output) {
		System.out.println(cf);
		KeySpaceMetadata.removeMetadata(cf);
	}
}
