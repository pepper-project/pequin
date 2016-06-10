package comp.jls;

import java.io.*;

import beaver.Scanner;
import beaver.Parser;

class Main
{
	public static void main(String[] args) throws Exception
	{
		JavaScanner input = new JavaScanner(new UnicodeEscapes(new FileReader(args[0])));
        JavaParser parser = new JavaParser();
		parser.parse(input);
	}
}
