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

public class DoCQL {
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
	public static void doCQL(String cql, int size, String[]... output)
			throws Exception {
		CQLStatement statement = QueryProcessor.getStatement(cql);
		switch (statement.type) {
		case SELECT:
			doSelect((SelectStatement) statement.statement, size, output);
			break;
		case INSERT:
			doInsert((UpdateStatement) statement.statement, size, output);
			break;
		case UPDATE:
			doUpdate((UpdateStatement) statement.statement, size, output);
			break;
		case CREATE_COLUMNFAMILY:
			doCreateColumnFamily(
					(CreateColumnFamilyStatement) statement.statement, size,
					output);
			break;
		case CREATE_INDEX:
			doCreateIndex((CreateIndexStatement) statement.statement, size,
					output);
			break;
		case DROP_INDEX:
			doDropIndex((DropIndexStatement) statement.statement, size, output);
			break;
		case DROP_COLUMNFAMILY:
			doDropColumnFamily((String) statement.statement, size, output);
			break;
		default:
			// NOT supported yet.
			throw new NotImplementedException();
		}
	}

	/**
	 * transform a given CQL select statement into put/get. Currently USING
	 * clause will be ignored. using wildchar is not supported yet. There is
	 * only one default keyspace. assume that an index for every column exist.
	 * key range not supported yet. multiple where clause not supported yet.
	 * 
	 * @param output
	 * 
	 * @param s
	 * @param size
	 * @return
	 */
	private static void doSelect(SelectStatement s, int size,
			String[]... output) {
		if (s.isWildcard()) {
			throw new NotImplementedException();
		}
		Metadata metadata = KeySpaceMetadata.getMetadata(s.getColumnFamily());
		List<Term> columns = s.getColumnNames();
		// Set<Term> keys = s.getKeys();
		List<Relation> relations = s.getRelations();

		if (relations.size() != 1) {
			throw new NotImplementedException(
					"only support exactly 1 where clause queries");
		}
		for (Relation relation : relations) {
			String value = relation.getValue().getText();
			String entity = relation.getEntity().getText();

			int[] result = API.treeRange(entity + "_index", value,
					relation.operator(), s.getNumRecords());

			// just call range query to get the results.
			assert (output.length == columns.size());

			for (int j = 0; j < columns.size(); j++) {
				String columnName = columns.get(j).getText();
				ColumnMetadata cm = metadata.getColumn(columnName);
				for (int i = 0; i < result.length; i++) {
					if (result[i] != -1) {
						output[i][j] = API.ramGet(cm.offset + cm.size
								* result[i]);
					}
				}
			}
			// testing.
			for (int i = 0; i < result.length; i++) {
				for (int j = 0; j < columns.size(); j++) {
					System.out.printf("%s: %s ", columns.get(j).getText(),
							output[i][j]);
				}
				System.out.println();
			}
			break;
		}
	}

	private static void doInsert(UpdateStatement s, int size,
			String[]... output) {
		Metadata metadata = KeySpaceMetadata.getMetadata(s.getColumnFamily());
		List<Term> columns = s.getColumnNames();
		List<Term> values = s.getColumnValues();
		// get value of key
		String key = s.getKeys().get(0).getText();
		// search on primary key
		int keyIndex = API.treeFind("KEY_index", key);
		if (keyIndex != -1) {
			// if exist, update
			int nextRowID = keyIndex;
			// calculate the offset and insert into it
			for (int i = 0; i < columns.size(); i++) {
				String columnName = columns.get(i).getText();
				String columnValue = values.get(i).getText();
				ColumnMetadata cm = metadata.getColumn(columnName);
				int columnOffset = cm.offset + nextRowID * cm.size;

				String columnOldValue = API.ramGet(columnOffset);
				if (!columnOldValue.equals(columnValue)) {
					API.ramPut(columnOffset, columnValue);
					// update index
					if (cm.indexFlag == 1) {
						API.treeDelete(columnName + "_index", columnOldValue);
						API.treeInsert(columnName + "_index", columnValue,
								nextRowID);
					}
				}
			}
		} else {
			// otherwise, find the next available slot
			int nextRowID = Integer.parseInt(API.ramGet(DB_NUM_OF_ROW_OFFSET));
			// update number of rows.
			API.ramPut(DB_NUM_OF_ROW_OFFSET, String.valueOf(nextRowID + 1));
			{
				// the column id for primary key is always 0.
				ColumnMetadata cm = metadata.getColumn("KEY");
				int columnOffset = cm.offset + nextRowID * cm.size;
				API.ramPut(columnOffset, key);
				API.treeInsert("KEY_index", key, nextRowID);
			}
			// calculate the offset and insert into it
			for (int i = 0; i < columns.size(); i++) {
				String columnName = columns.get(i).getText();
				String columnValue = values.get(i).getText();
				ColumnMetadata cm = metadata.getColumn(columnName);
				int columnOffset = cm.offset + nextRowID * cm.size;

				API.ramPut(columnOffset, columnValue);

				// update index
				if (cm.indexFlag == 1) {
					API.treeInsert(columnName + "_index", columnValue,
							nextRowID);
				}
			}
		}
	}

	private static void doUpdate(UpdateStatement s, int size,
			String[]... output) {
		// TODO similar to insert
		// return s.toString();
	}

	private static void doCreateColumnFamily(CreateColumnFamilyStatement s,
			int size, String[]... output) {
		System.out.printf("name: %s\n", s.getName());
		Metadata metadata = new Metadata(s.getName());

		API.ramPut(DB_SIZE_OFFSET, String.valueOf(size));
		API.ramPut(DB_NUM_OF_ROW_OFFSET, "0");

		// the first column should be the primary key.
		// column name: KEY
		int currentColumnOffset = DB_DATA_OFFSET;
		int columnSize = 1;

		ColumnMetadata cm = new ColumnMetadata("KEY", s.getKeyType(),
				currentColumnOffset, columnSize, 1);
		metadata.addColumn(cm);

		currentColumnOffset += columnSize * size;
		for (Entry<Term, String> column : s.getColumns().entrySet()) {
			System.out.printf("column name: %s type: %s\n", column.getKey(),
					column.getValue());

			columnSize = 1;
			cm = new ColumnMetadata(column.getKey().getText(),
					column.getValue(), currentColumnOffset, columnSize, 0);
			metadata.addColumn(cm);

			currentColumnOffset += columnSize * size;
		}
		KeySpaceMetadata.addMetadata(s.getName(), metadata);
	}

	private static void doCreateIndex(CreateIndexStatement s, int size,
			String[]... output) {
		// get column meta data
		Metadata metadata = KeySpaceMetadata.getMetadata(s.getColumnFamily());
		String columnName = s.getColumnName().getText();
		ColumnMetadata cm = metadata.getColumn(columnName);

		// insert them into the tree.
		String indexName = columnName + "_index";
		int numberOfRows = Integer.parseInt(API.ramGet(DB_NUM_OF_ROW_OFFSET));
		for (int i = 0; i < numberOfRows; i++) {
			String columnValue = API.ramGet(cm.offset + i * cm.size);
			API.treeInsert(indexName, columnValue, i);
		}
		cm.indexFlag = 1;
		
	}

	private static void doDropIndex(DropIndexStatement statement, int size,
			String[]... output) {
		// TODO Auto-generated method stub
		throw new NotImplementedException();
	}

	private static void doDropColumnFamily(String cf, int size,
			String[]... output) {
		System.out.println(cf);
		KeySpaceMetadata.removeMetadata(cf);
	}
}
