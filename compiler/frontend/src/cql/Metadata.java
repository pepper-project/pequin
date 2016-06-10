import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Metadata {
	private HashMap<String, ColumnMetadata> columnsMap;
	private List<ColumnMetadata> columnsList;
	private String name;
	
	public Metadata(String name) {
		this.name = name;
		this.columnsMap = new HashMap<String, ColumnMetadata>();
		this.columnsList = new ArrayList<ColumnMetadata>();
	}

	public void addColumn(ColumnMetadata column) {
		columnsMap.put(column.name, column);
		columnsList.add(column);
	}
	
	public String getName() {
		return name;
	}

	public ColumnMetadata getColumn(String name) {
		return columnsMap.get(name);
	}

	public List<ColumnMetadata> getColumns() {
		return columnsList;
	}
}
