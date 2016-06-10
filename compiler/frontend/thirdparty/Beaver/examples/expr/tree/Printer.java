package expr.tree;

import java.io.IOException;
import java.io.StringReader;

import beaver.Symbol;
import beaver.Parser;

import expr.tree.ast.*;

class Printer
{
	static public void main(String[] args) throws IOException, Parser.Exception
	{
		ExpressionParser parser = new ExpressionParser();
		ExpressionScanner input = new ExpressionScanner(new StringReader(args[0]));
		Expr expr = (Expr) parser.parse(input);
		TreeWalker printer = new TreeWalker()
		{
			int tab = 0;

			void startLine(Expr expr)
			{
				System.out.print(Symbol.getLine(expr.getStart()));
				System.out.print(',');
				System.out.print(Symbol.getColumn(expr.getStart()));
				System.out.print('-');
				System.out.print(Symbol.getLine(expr.getEnd()));
				System.out.print(',');
				System.out.print(Symbol.getColumn(expr.getEnd()));
				System.out.print(":\t");
				for (int i = 0; i < tab; i++)
					System.out.print("  ");
			}

			public void visit(ErrExpr expr)
			{
				startLine(expr);
				System.out.print(" [E] ");
				System.out.println("*** error ***");
			}
			
			public void visit(NumExpr expr)
			{
				startLine(expr);
				System.out.print(" [=] ");
				System.out.println(expr.value);
			}

			public void visit(NegExpr expr)
			{
				startLine(expr);
				System.out.println(" +/- ");

				tab++;
				super.visit(expr);
				tab--;
			}

			public void visit(MultExpr expr)
			{
				startLine(expr);
				System.out.println(" [*] ");

				tab++;
				super.visit(expr);
				tab--;
			}

			public void visit(DivExpr expr)
			{
				startLine(expr);
				System.out.println(" [/] ");

				tab++;
				super.visit(expr);
				tab--;
			}

			public void visit(PlusExpr expr)
			{
				startLine(expr);
				System.out.println(" [+] ");

				tab++;
				super.visit(expr);
				tab--;
			}

			public void visit(MinusExpr expr)
			{
				startLine(expr);
				System.out.println(" [-] ");

				tab++;
				super.visit(expr);
				tab--;
			}
		};
		expr.accept(printer);
	}
}