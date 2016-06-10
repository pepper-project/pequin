package expr.eval;

import java.io.IOException;
import java.io.StringReader;
import beaver.Symbol;
import beaver.Parser;

public class Calculator
{
	public static void main(String[] args)
	{
		ExpressionScanner input = new ExpressionScanner(new StringReader(args[0]));
		try
		{
			Expr result = (Expr) new ExpressionParser().parse(input);
			System.out.println("= " + result.val);
		}
		catch (IOException e)
		{
			System.err.println("Failed to read expression: " + e.getMessage());
		}
		catch (Parser.Exception e)
		{
			System.err.println("Invalid expression: " + e.getMessage());
		}
	}
}
