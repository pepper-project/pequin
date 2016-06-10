public class ColumnMetadata {
	String name;
	String type;
	int offset;
	int size;
	int indexFlag;

	public ColumnMetadata(String name, String type, int offset, int size,
	    int indexFlag) {
		this.name = name;
		this.type = type;
		this.offset = offset;
		this.size = size;
		this.indexFlag = indexFlag;
	}

	public ColumnMetadata(String name, String type, int offset, int indexFlag) {
		this.name = name;
		this.type = type;
		this.offset = offset;
		this.indexFlag = indexFlag;
	}

	public ColumnMetadata(String name, String type, int indexFlag) {
		// TODO Auto-generated constructor stub
		this.name = name;
		this.type = type;
		this.indexFlag = indexFlag;
	}
}