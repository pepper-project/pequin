package path;

import beaver.*;
import java.io.*;
import java.util.List;
import java.util.Iterator;

class Run
{
	static public void main(String[] args) throws Exception
	{
		int path_index = 0;
		short alt_goal = 0;
		if (args[0].equals("-p"))
		{
			alt_goal = PathParser.AltGoals.pathname;
			path_index = 1;
		}
		else if (args[0].equals("-f"))
		{
			alt_goal = PathParser.AltGoals.filename;
			path_index = 1;
		}
		Scanner lexer = new PathScanner(new StringReader(args[path_index]));
		Parser parser = new PathParser();
		List namelist = alt_goal != 0 ? (List) parser.parse(lexer, alt_goal) : (List) parser.parse(lexer);

		for (Iterator i = namelist.iterator(); i.hasNext(); ) {
			System.out.print(i.next());
			System.out.print(' ');
		}
		System.out.print("\nDone.");
	}
}
