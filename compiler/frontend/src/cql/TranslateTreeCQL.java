import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.cassandra.cql.CQLStatement;
import org.apache.cassandra.cql.CreateColumnFamilyStatement;
import org.apache.cassandra.cql.CreateIndexStatement;
import org.apache.cassandra.cql.DropIndexStatement;
import org.apache.cassandra.cql.QueryProcessor;
import org.apache.cassandra.cql.Relation;
import org.apache.cassandra.cql.RelationType;
import org.apache.cassandra.cql.SelectStatement;
import org.apache.cassandra.cql.Term;
import org.apache.cassandra.cql.UpdateStatement;
import org.apache.cassandra.cql3.statements.DropColumnFamilyStatement;
import org.apache.cassandra.exceptions.InvalidRequestException;
import org.apache.commons.lang.NotImplementedException;

/**
 * A database handle is going to be a struct containing a hash to the index on
 * each column. For example, the handle for Student DB will be:
 * 
 * struct Student_handle { hash_t KEY_index; hash_t Major_index; hash_t
 * LName_index; hash_t State_index; hash_t Age_index; hash_t Class_index; hash_t
 * FName_index; hash_t Credits_index; hash_t Average_index; hash_t
 * PhoneNum_index; } Student_handle_t;
 * 
 * And the input to every DB computation will be the same - the DB handle. The
 * output to every readonly DB computation will be the results. The output to
 * every read/write DB computation will be the new DB handle.
 * 
 * Sample Queries:
 * 
 * SELECT * FROM Student WHERE Average >= 90 LIMIT 5
 * 
 * INSERT INTO Student (KEY, FName, LName, Major, Average) VALUES (tempKEY,
 * tempFName, tempLName, tempMajor, tempAverage)
 * 
 * CREATE INDEX ON Student (Age)
 * 
 * @author ren
 * 
 */
public class TranslateTreeCQL {
	private static void printDBHandle(Metadata metadata) {
		List<ColumnMetadata> columns = metadata.getColumns();
		System.out.printf("typedef struct %s_handle {\n", metadata.getName());
		for (ColumnMetadata column : columns) {
			System.out.printf("    hash_t %s_index;\n", column.name);
		}
		System.out.printf("} %s_handle_t;\n", metadata.getName());
	}

	private static void printTypedef(Metadata metadata) {
		List<ColumnMetadata> columns = metadata.getColumns();
		for (ColumnMetadata column : columns) {
			System.out.printf("typedef %s %s_t;\n", column.type, column.name);
		}
	}

	private static void printRowStruct(Metadata metadata) {
		List<ColumnMetadata> columns = metadata.getColumns();
		System.out.printf("typedef struct %s {\n", metadata.getName());
		for (ColumnMetadata column : columns) {
			System.out.printf("    %s_t %s;\n", column.name, column.name);
		}
		System.out.printf("} %s_t;\n", metadata.getName());
	}

	private static void printResultsStruct(String cfName, List<Term> columns) {
		System.out.printf("typedef struct %s_result {\n", cfName);
		for (Term column : columns) {
			System.out.printf("   %s_t %s;\n", column.getText(), column.getText());
		}
		System.out.printf("} %s_result_t;\n", cfName);
	}

	/**
	 * 
	 * The user should make sure that output is enough to hold the result of
	 * select statement. The amount of rows returned by select is by default 5
	 * rows. To get more rows, use LIMIT <number> clause inside select statement.
	 * In either case, the size of the array to hold return result should be at
	 * least <number of rows>.
	 * 
	 * @param cql
	 * @param params
	 * @throws Exception
	 */
	public static void tranlsateCQL(String cql, String... params)
	    throws Exception {
		CQLStatement statement = QueryProcessor.getStatement(cql);
		System.out.printf("%s//%s\n", PrintAPI.getIndent(), cql);
		System.out.printf("#ifdef DEBUG\n");
		System.out.printf("%sprintf(\"%s\\n\");\n", PrintAPI.getIndent(), cql);
		System.out.printf("#endif\n");

		switch (statement.type) {
		case SELECT:
			translateSelect((SelectStatement) statement.statement, params);
			break;
		case INSERT:
			translateInsert((UpdateStatement) statement.statement, params);
			break;
		case UPDATE:
			translateUpdate((UpdateStatement) statement.statement, params);
			break;
		case CREATE_COLUMNFAMILY:
			translateCreateColumnFamily(
			    (CreateColumnFamilyStatement) statement.statement, params);
			break;
		case CREATE_INDEX:
			translateCreateIndex((CreateIndexStatement) statement.statement, params);
			break;
		case DROP_INDEX:
			translateDropIndex((DropIndexStatement) statement.statement, params);
			break;
		case DROP_COLUMNFAMILY:

			translateDropColumnFamily(
			    (DropColumnFamilyStatement) statement.statement, params);
			break;
		default:
			// NOT supported yet.
			throw new NotImplementedException();
		}
	}

	/**
	 * Translate a CQL query into a full piece of C code that can be run and give
	 * output to the client. It is useful to build a command line interpreter for
	 * CQL functionality. We do so by first generate struct definitions based on
	 * local metadata (which is created by CREATE COLUMNFAMILY query and updated
	 * by a couple of other queries), the compute function and the code piece that
	 * perform the queries and finally preparing the output.
	 * 
	 */
	public static void translateCQLIntoFullCode(String cql) throws Exception {
		CQLStatement statement = QueryProcessor.getStatement(cql);
		List<String> params = new ArrayList<String>();

		System.out.printf("#include <stdint.h>\n");
		System.out.printf("#include <db.h>\n");
		System.out.printf("#include <avl_tree.h>\n");

		switch (statement.type) {
		case SELECT: {
			SelectStatement s = (SelectStatement) statement.statement;
			translateSelectCLI(s, "output->result");
			break;
		}
		case INSERT: {
			UpdateStatement s = (UpdateStatement) statement.statement;
			translateInsertCLI(s, params.toArray(new String[] {}));
			break;
		}
		case UPDATE: {
			UpdateStatement s = (UpdateStatement) statement.statement;
			translateUpdateCLI(s, params.toArray(new String[] {}));
			break;
		}
		case CREATE_COLUMNFAMILY: {
			CreateColumnFamilyStatement s = (CreateColumnFamilyStatement) statement.statement;
			translateCreateColumnFamilyCLI(s, params.toArray(new String[] {}));
			break;
		}
		case DROP_INDEX: {
			DropIndexStatement s = (DropIndexStatement) statement.statement;
			translateDropIndexCLI(s, params.toArray(new String[] {}));
			break;
		}
		case DROP_COLUMNFAMILY: {
			DropColumnFamilyStatement s = (DropColumnFamilyStatement) statement.statement;
			translateDropColumnFamilyCLI(s, params.toArray(new String[] {}));
			break;
		}
		case CREATE_INDEX: {
			// NOT supported yet.
			throw new NotImplementedException();
			// CreateIndexStatement s = (CreateIndexStatement) statement.statement;
			// translateCreateIndexCLI(s, output.toArray(new String[] {}));
			// break;
		}
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
	 * @param s
	 * @param params
	 * 
	 * @return
	 */

	// TODO add a database structure which stores root hash of each column's
	// index.
	private static void translateSelectCLI(SelectStatement s, String... params) {
		String cfName = s.getColumnFamily();
		Metadata metadata = KeySpaceMetadata.getMetadata(cfName);
		List<Relation> relations = s.getRelations();
		String entity = relations.get(0).getEntity().getText();

		// generate typedef
		printTypedef(metadata);
		// generate struct definition for the DB handle
		printDBHandle(metadata);
		// generate strcut definition for the row
		printRowStruct(metadata);
		// generate struct definition to hold select results.
		List<Term> columns = s.getColumnNames();
		printResultsStruct(cfName, columns);

		// In, Out struct definition.
		System.out.printf("struct In {%s_handle_t handle;};\n", cfName);
		System.out.printf("struct Out {%s_result_t result[%d];};\n", cfName,
		    s.getColumnsLimit());

		// body of compute function
		System.out.printf("int compute(struct In *input, struct Out *output) {\n");

		// declare and initialize variables
		System.out.printf("    tree_t %s_index;\n", entity);
		System.out.printf("    %s_index.root = input->handle->%s_index;\n", entity,
		    entity);

		// generate code snippet that actually do the computation
		translateSelect(s, params);

		// finish the body of compute function.
		System.out.printf("    return 0;\n");
		System.out.printf("}\n");
	}

	// The semantics of insert and update are the same.
	private static void translateInsertCLI(UpdateStatement s, String... params)
	    throws InvalidRequestException {
		translateUpdateCLI(s, params);
	}

	private static void translateUpdateCLI(UpdateStatement s, String... params)
	    throws InvalidRequestException {
		String cfName = s.getColumnFamily();
		Metadata metadata = KeySpaceMetadata.getMetadata(cfName);

		List<ColumnMetadata> columnMetadata = metadata.getColumns();

		// generate typedef
		printTypedef(metadata);
		// generate struct definition for the DB handle
		printDBHandle(metadata);
		// generate strcut definition for the row
		printRowStruct(metadata);

		// In, Out struct definition.
		System.out.printf("struct In {%s_handle_t handle;};\n", cfName);
		System.out.printf("struct Out {%s_handle_t handle;};\n", cfName);

		// body of compute function
		System.out.printf("int compute(struct In *input, struct Out *output) {\n");

		// declare and initialize variables
		for (ColumnMetadata column : columnMetadata) {
			if (column.indexFlag == 1) {
				System.out.printf("    tree_t %s_index;\n", column.name);
			}
		}

		for (ColumnMetadata column : columnMetadata) {
			if (column.indexFlag == 1) {
				System.out.printf("    %s_index.root = input->handle.%s_index;\n",
				    column.name, column.name);
			}
		}

		// generate code snippet that actually do the computation
		translateUpdate(s, params);

		// update output
		for (ColumnMetadata column : columnMetadata) {
			if (column.indexFlag == 1) {
				System.out.printf("    output->handle.%s_index = %s_index.root;\n",
				    column.name, column.name);
			} else {
				System.out.printf(
				    "    output->handle.%s_index = input->handle.%s_index;\n",
				    column.name, column.name);
			}
		}
		// finish the body of compute function.
		System.out.printf("    return 0;\n");
		System.out.printf("}\n");
	}

	private static void translateCreateColumnFamilyCLI(
	    CreateColumnFamilyStatement s, String... output) {
		translateCreateColumnFamily(s, output);
	}

	private static void translateCreateIndexCLI(CreateIndexStatement s,
	    String... params) {
		String cfName = s.getColumnFamily();
		Metadata metadata = KeySpaceMetadata.getMetadata(cfName);

		// generate typedef
		printTypedef(metadata);
		// generate struct definition for the DB handle
		printDBHandle(metadata);
		// generate strcut definition for the row
		printRowStruct(metadata);

		// In, Out struct definition.
		System.out.printf("struct In {%s_handle_t handle;};\n", cfName);
		System.out.printf("struct Out {%s_handle_t handle;};\n", cfName);

		// body of compute function
		System.out.printf("int compute(struct In *input, struct Out *output) {\n");

		// declare and initialize variables
		System.out.printf("    tree_t KEY_index;\n");
		System.out.printf("    tree_t %s_index;\n", s.getColumnName().getText());
		System.out.printf("    KEY_index.root = input->handle.KEY_index;\n");
		System.out
		    .printf("    tree_init(%s_index);\n", s.getColumnName().getText());

		// generate code snippet that actually do the computation.
		translateCreateIndex(s, params);

		// update output
		System.out.printf("    output->handle = input->handle;\n");
		System.out.printf("    output->handle.%s_index = %s_index.root;\n", s
		    .getColumnName().getText(), s.getColumnName().getText());

		// finish the body of compute function.
		System.out.printf("    return 0;\n");
		System.out.printf("}\n");
	}

	private static void translateDropIndexCLI(DropIndexStatement s,
	    String... params) {
		translateDropIndex(s, params);
	}

	private static void translateDropColumnFamilyCLI(DropColumnFamilyStatement s,
	    String... params) {
		translateDropColumnFamily(s, params);
	}

	/**
	 * transform a given CQL select statement into put/get. Currently USING clause
	 * will be ignored. using wildchar is not supported yet. There is only one
	 * default keyspace. assume that an index for every column exist. key range
	 * not supported yet. multiple where clause not supported yet.
	 * 
	 * @param s
	 * @param params
	 * 
	 * @return
	 */
	private static void translateSelect(SelectStatement s, String... params) {
		Metadata metadata = KeySpaceMetadata.getMetadata(s.getColumnFamily());
		List<Term> columns = s.getColumnNames();
		List<Relation> relations = s.getRelations();
		String cfName = s.getColumnFamily();

		if (params.length < 1) {
			throw new NotImplementedException(
			    "You need to specify a variable to hold the content of the result.");
		}

		if (relations.size() == 0) {
			throw new NotImplementedException();
		}
		System.out.printf("%s{\n", PrintAPI.getIndent());
		PrintAPI.increaseIndent();
		{
			// declare temporary variables.
			System.out.printf("%sint i;\n", PrintAPI.getIndent());
			System.out.printf("%s%s_t temp%s;\n", PrintAPI.getIndent(), cfName,
			    cfName);
			System.out.printf("%stree_result_set_t tempResult;\n",
			    PrintAPI.getIndent());

			// there are more than one filter, they can only be on the same column
			if (relations.size() > 1) {
				if (relations.size() > 2) {
					throw new NotImplementedException(
					    "Cannot support so many filters (WHERE clauses).");
				}
				Relation firstRelation = relations.get(0);
				Relation secondRelation = relations.get(1);
				// we can support two filters on the same columns which specify both an
				// upper bound and a lower bound
				if (firstRelation.getEntity().getText()
				    .equals(secondRelation.getEntity().getText())) {
					String entity = firstRelation.getEntity().getText();
					if ((firstRelation.operator() != RelationType.GTE && firstRelation
					    .operator() != RelationType.GT)
					    || (secondRelation.operator() != RelationType.LTE && secondRelation
					        .operator() != RelationType.LT)) {
						throw new NotImplementedException(
						    "Lower bound should come before uppper bound.");
					} else {
						ColumnMetadata em = metadata.getColumn(entity);
						if (em.indexFlag == 0) {
							throw new NotImplementedException(
							    "Query on non-indexed column not supported.");
						}
						String lowerBound = firstRelation.getValue().getText();
						String upperBound = secondRelation.getValue().getText();
						String lowerBoundInclusive;
						String upperBoundInclusive;

						// lower bound
						switch (firstRelation.operator()) {
						case GT: {
							lowerBoundInclusive = "FALSE";
							break;
						}
						case GTE: {
							lowerBoundInclusive = "TRUE";
							break;
						}
						default:
							throw new NotImplementedException("Should not happen");
						}

						// uppper bound
						switch (secondRelation.operator()) {
						case LT: {
							upperBoundInclusive = "FALSE";
							break;
						}
						case LTE: {
							upperBoundInclusive = "TRUE";
							break;
						}
						default:
							throw new NotImplementedException("Should not happen");
						}
						PrintAPI.treeFind("tempResult", entity + "_index", lowerBound,
						    lowerBoundInclusive, upperBound, upperBoundInclusive,
						    s.getNumRecords());
					}
				} else {
					throw new NotImplementedException(
					    "Two relations must specify an upper bound first and then a lower bound on the same column.");
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

				// just call range query to get the results.
				PrintAPI.treeFind("tempResult", entity + "_index", value,
				    relation.operator(), s.getNumRecords());
			}
			// Fetch select results and copy them into output variable.
			System.out.printf("%sfor (i = 0; i < %d; i++) {\n", PrintAPI.getIndent(),
			    s.getNumRecords());
			PrintAPI.increaseIndent();
			{
				System.out.printf("%sif (i < tempResult.num_results) {\n",
				    PrintAPI.getIndent());
				PrintAPI.increaseIndent();
				{
					PrintAPI.hashGet("temp" + cfName, "tempResult.results[i].value");
					if (s.isWildcard()) {
						for (ColumnMetadata cm : metadata.getColumns()) {
							String columnName = cm.name;
							PrintAPI.assignment(params[0] + "[i]." + columnName, "temp"
							    + cfName + "." + columnName, cm.type, false);
						}
					} else {
						for (int j = 0; j < columns.size(); j++) {
							String columnName = columns.get(j).getText();
							ColumnMetadata cm = metadata.getColumn(columnName);
							PrintAPI.assignment(params[0] + "[i]." + columnName, "temp"
							    + cfName + "." + columnName, cm.type, false);
						}
					}
				}
				PrintAPI.decreaseIndent();
				System.out.printf("%s} else {\n", PrintAPI.getIndent());
				PrintAPI.increaseIndent();
				{
					if (s.isWildcard()) {
						for (ColumnMetadata cm : metadata.getColumns()) {
							String columnName = cm.name;
							PrintAPI.assignment(params[0] + "[i]." + columnName, null,
							    cm.type, true);
						}
					} else {
						for (int j = 0; j < columns.size(); j++) {
							String columnName = columns.get(j).getText();
							ColumnMetadata cm = metadata.getColumn(columnName);
							PrintAPI.assignment(params[0] + "[i]." + columnName, null,
							    cm.type, true);
						}
					}
				}
				PrintAPI.decreaseIndent();
				System.out.printf("%s}\n", PrintAPI.getIndent());
			}
			PrintAPI.decreaseIndent();
			System.out.printf("%s}\n", PrintAPI.getIndent());
			// put the number of results in the second output variable.
			if (params.length > 1) {
				System.out.printf("%s%s = tempResult.num_results;\n",
				    PrintAPI.getIndent(), params[1]);
			}
		}
		PrintAPI.decreaseIndent();
		System.out.printf("%s}\n", PrintAPI.getIndent());
	}

	// The semantics of insert and update are the same.
	private static void translateInsert(UpdateStatement s, String... params)
	    throws InvalidRequestException {
		translateUpdate(s, params);
	}

	private static void translateUpdate(UpdateStatement s, String... params)
	    throws InvalidRequestException {
		Metadata metadata = KeySpaceMetadata.getMetadata(s.getColumnFamily());
		String cfName = s.getColumnFamily();
		Set<Term> columns = s.getColumns().keySet();

		if (params.length < 2) {
			throw new NotImplementedException(
			    "You need to specify two variables to carry the path to the node that is updated.");
		}

		for (Term keyTerm : s.getKeys()) {
			String key = keyTerm.getText();

			System.out.printf("%s{\n", PrintAPI.getIndent());
			PrintAPI.increaseIndent();
			{
				// declare temporary variables.
				System.out.printf("%stree_result_set_t tempResult;\n",
				    PrintAPI.getIndent());
				System.out.printf("%s%s_t temp%s;\n", PrintAPI.getIndent(), cfName,
				    cfName);
				System.out.printf("%shash_t tempHash;\n", PrintAPI.getIndent());
				System.out.printf("%shash_t oldHash;\n", PrintAPI.getIndent());

				// try to fetch the row first
				PrintAPI.treeFind("tempResult", "KEY_index", key, RelationType.EQ, 1);

				// if it does not exist
				System.out.printf("%sif (tempResult.num_results == 0) {\n",
				    PrintAPI.getIndent());
				PrintAPI.increaseIndent();
				{
					ColumnMetadata cm = metadata.getColumn("KEY");
					PrintAPI.assignment("temp" + cfName + ".KEY", key, cm.type, true);
					PrintAPI.assignment("oldHash", "*NULL_HASH", "hash_t", false);
				}
				PrintAPI.decreaseIndent();
				// if it exists
				System.out.printf("%s} else {\n", PrintAPI.getIndent());
				PrintAPI.increaseIndent();
				{
					// fetch the current row
					PrintAPI.hashGet("temp" + cfName, "tempResult.results[0].value");
					PrintAPI.assignment("oldHash", "tempResult.results[0].value",
					    "hash_t", false);
				}
				PrintAPI.decreaseIndent();
				System.out.printf("%s}\n", PrintAPI.getIndent());

				// fill fields through assignments
				for (Term column : columns) {
					String columnName = column.getText();
					String columnValue = s.getColumns().get(column).a.getText();
					ColumnMetadata cm = metadata.getColumn(columnName);

					PrintAPI.assignment("temp" + cfName + "." + columnName, columnValue,
					    cm.type, true);
				}
				// insert the row into the database through a hashput.
				PrintAPI.hashPut("tempHash", "temp" + cfName);
				// update all indexes.
				PrintAPI.treeUpdateNoBalance("KEY_index", "temp" + cfName + ".KEY",
				    "oldHash", "tempHash", params[0], params[1]);
				for (Term column : columns) {
					String columnName = column.getText();
					ColumnMetadata cm = metadata.getColumn(columnName);
					if (cm.indexFlag == 1) {
						PrintAPI
						    .treeUpdateNoBalance(columnName + "_index", "temp" + cfName
						        + "." + columnName, "oldHash", "tempHash", params[0],
						        params[1]);
					}
				}
			}
			PrintAPI.decreaseIndent();
			System.out.printf("%s}\n", PrintAPI.getIndent());
		}
	}

	private static void translateCreateColumnFamily(
	    CreateColumnFamilyStatement s, String... params) {
		Metadata metadata = new Metadata(s.getName());

		// the first column should be the primary key.
		// column name: KEY
		ColumnMetadata cm = new ColumnMetadata("KEY", s.getKeyType(), 1);
		metadata.addColumn(cm);

		int i = 0;
		for (Entry<Term, String> column : s.getColumns().entrySet()) {
			cm = new ColumnMetadata(column.getKey().getText(), column.getValue(), i,
			    0);
			metadata.addColumn(cm);
			i++;
		}
		KeySpaceMetadata.addMetadata(s.getName(), metadata);
	}

	private static void translateCreateIndex(CreateIndexStatement s,
	    String... params) {
		// traverse the tree of the KEY_index to create a new index.

		// get column meta data
		String cfName = s.getColumnFamily();

		Metadata metadata = KeySpaceMetadata.getMetadata(cfName);
		String columnName = s.getColumnName().getText();
		String indexName = columnName + "_index";

		ColumnMetadata cm = metadata.getColumn(columnName);

		if (params.length < 1) {
			throw new NotImplementedException(
			    "You should specify the size of the DB to create index.");
		}

		int size = Integer.parseInt(params[0]);

		System.out.printf("%s{\n", PrintAPI.getIndent());
		PrintAPI.increaseIndent();
		{
			// declare temporary variables
			System.out.printf("%sBOOL found = FALSE;\n", PrintAPI.getIndent());
			System.out.printf("%s%s_t temp%s;\n", PrintAPI.getIndent(), cfName,
			    cfName);
			System.out.printf("%stree_path_t path;\n", PrintAPI.getIndent());

			// initialize the tree which stores the index.
			System.out.printf("%stree_init(&(%s));\n", PrintAPI.getIndent(),
			    indexName);

			// find first path.
			System.out.printf("%sfound = _find_first(&(KEY_index), &(path);\n",
			    PrintAPI.getIndent());

			// insert each row into the index tree.
			System.out.printf("%sfor (i = 0; i < %d; i++) {\n", PrintAPI.getIndent(),
			    size);
			PrintAPI.increaseIndent();
			{
				System.out.printf("%sif (found) {\n", PrintAPI.getIndent());
				PrintAPI.increaseIndent();
				{
					System.out.printf("%sWITH_PATH_NODE_BEGIN((&path))\n",
					    PrintAPI.getIndent());
					// fetch the content of the node through a hashget
					PrintAPI.hashGet("temp" + cfName, "node->values[0]");
					// insert the node into the new index tree.
					PrintAPI.treeInsert(indexName, "temp" + cfName + "." + columnName,
					    "node->valules[0]");
					System.out.printf("%sWITH_PATH_NODE_END\n", PrintAPI.getIndent());
					System.out.printf("%sfound = _find_next(&(path);\n",
					    PrintAPI.getIndent());
				}
				PrintAPI.decreaseIndent();
				System.out.printf("%s}\n", PrintAPI.getIndent());
			}
			PrintAPI.decreaseIndent();
			System.out.printf("%s}\n", PrintAPI.getIndent());
		}
		PrintAPI.decreaseIndent();
		System.out.printf("%s}\n", PrintAPI.getIndent());

		cm.indexFlag = 1;
		KeySpaceMetadata.addMetadata(cfName, metadata);
	}

	private static void translateDropIndex(DropIndexStatement s, String... params) {
		// mark the column as unindexed in local metadata.
		try {
			String cfName = s.getColumnFamily();
			Metadata metadata = KeySpaceMetadata.getMetadata(cfName);
			String columnName = s.indexName.substring(0,
			    s.indexName.indexOf("_index"));
			ColumnMetadata cm = metadata.getColumn(columnName);
			cm.indexFlag = 0;
			KeySpaceMetadata.addMetadata(cfName, metadata);
		} catch (InvalidRequestException e) {
			e.printStackTrace();
		}
	}

	private static void translateDropColumnFamily(DropColumnFamilyStatement cf,
	    String... params) {
		System.out.println(cf.columnFamily());
		KeySpaceMetadata.removeMetadata(cf.columnFamily());
	}
}
