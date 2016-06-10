import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CQLToKVStore {
	public static void main(String[] args) {
		// prepare some key-value store for testing purpose.
		Pattern pattern = Pattern
		    .compile("CQL\\(\\s*\"([^\"]*)\"((?:,\\s*([^\\s]+)\\s*)*\\s*)\\)\\s*;");
		Pattern pattern1 = Pattern.compile(",\\s*([^\\s,]+)");
		Scanner sc = new Scanner(System.in);
		while (sc.hasNext()) {
			String line = sc.nextLine();
			// the function CQL(query, size_db) will be translated into a
			// bunch of put/get.
			Matcher matcher = pattern.matcher(line.trim());

			if (matcher.matches()) {
				try {
					String cql = matcher.group(1);
					// DoCQL.doCQL(matcher.group(1), size, new String[10][10]);

					String outputHolder = matcher.group(2);
					matcher = pattern1.matcher(outputHolder);
					List<String> output = new ArrayList<String>();
					while (matcher.find()) {
						// System.out.println(matcher.group(1));
						output.add(matcher.group(1));
					}
					TranslateTreeCQL.tranlsateCQL(cql, output.toArray(new String[] {}));
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				System.out.println(line);
			}
		}
	}
}
