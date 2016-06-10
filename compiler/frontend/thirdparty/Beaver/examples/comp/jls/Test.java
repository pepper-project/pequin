package comp.jls;

import java.io.*;

import beaver.Scanner;
import beaver.Parser;

class Test
{
	public static void main(String[] args) throws Exception
	{
        int count = Integer.parseInt(args[0]);

        char[][] txts = new char[args.length - 1][];
        for (int i = 1; i < args.length; i++)
        {
            String name = args[i];
            File src = new File(name);
            txts[i - 1] = new char[(int) src.length()];
            Reader txt_reader = new FileReader(src);
            try
            {
                txt_reader.read(txts[i - 1]);
            }
            finally
            {
                txt_reader.close();
            }
        }

        JavaParser parser = new JavaParser();
        for (int n = 0; n < txts.length; n++)
        {
			JavaScanner input = new JavaScanner(new UnicodeEscapes(new CharArrayReader(txts[n])));
			parse(input, parser);
		}
		System.gc();

		long tt = 0;
		for (int i = 0; i < count; i++)
		{
			long ct = 0;
            for (int n = 0; n < txts.length; n++)
            {
                JavaScanner input = new JavaScanner(new UnicodeEscapes(new CharArrayReader(txts[n])));
	    		long dt = parse(input, parser);
	    		ct += dt;
	    		tt += dt;
            }
    		//System.out.print((i + 1) + ": " + ct + "ms\t\t\r");
		    System.gc();
		}
        System.out.println(txts.length + " files parsed " + count + " times in " + tt + "ms (avg=" + (((double) tt) / count) + "ms)");
	}

	private static long parse(JavaScanner input, JavaParser parser)
	{
		long t0 = System.currentTimeMillis();
		try
		{
			parser.parse(input);
		}
		catch (IOException e)
		{
			System.err.println("Error reading input: " + e.getMessage());
		}
		catch (Parser.Exception e)
		{
			System.err.println("Bad expression: " + e.getMessage());
		}
		return System.currentTimeMillis() - t0;
	}
}
