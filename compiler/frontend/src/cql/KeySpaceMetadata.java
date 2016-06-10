import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;

public class KeySpaceMetadata {
	// private static HashMap<String, Metadata> globalMetadata = new
	// HashMap<String, Metadata>();

	static {
		// read meta data from
	}

	public static Metadata getMetadata(String cfName) {
		// return globalMetadata.get(cfName);
		return readMetadata(cfName);
	}

	public static void addMetadata(String cfName, Metadata metadata) {
		// globalMetadata.put(cfName, metadata);
		updateMetadata(cfName, metadata);
	}

	public static void removeMetadata(String cfName) {
		// globalMetadata.remove(cfName);
	}

	public static Metadata readMetadata(String cfName) {
		try {
			// use buffering
			FileReader fs = new FileReader(cfName);
			BufferedReader br = new BufferedReader(fs);
			Metadata md = new Metadata(cfName);
			while (true) {
				String line = br.readLine();
				if (line == null) {
					break;
				}
				String[] columns = line.split(",");
				assert columns.length == 5;
				String name = columns[0];
				String type = columns[1];
				int offset = Integer.parseInt(columns[2]);
				int size = Integer.parseInt(columns[3]);
				int indexFlag = Integer.parseInt(columns[4]);
				ColumnMetadata cm = new ColumnMetadata(name, type, offset, size,
				    indexFlag);
				md.addColumn(cm);
			}
			fs.close();
			return md;
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static void updateMetadata(String cfName, Metadata metadata) {
		try {
			FileWriter fw = new FileWriter(cfName);
			PrintWriter pw = new PrintWriter(fw);
			List<ColumnMetadata> columns = metadata.getColumns();
			for (ColumnMetadata cm : columns) {
				String line = cm.name + "," + cm.type + "," + cm.offset + "," + cm.size
				    + "," + cm.indexFlag;
				pw.println(line);
			}
			fw.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
