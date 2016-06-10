import java.util.Scanner;

public class CQLCLI {
	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		while (sc.hasNext()) {
			String cql = sc.nextLine();
			try {
				TranslateTreeCQL.translateCQLIntoFullCode(cql);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
