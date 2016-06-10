package expr.tree;

import java.io.IOException;
import java.io.StringReader;

import beaver.Symbol;
import beaver.Parser;

import expr.tree.ast.*;

class Calculator
{
	static class Accumulator extends TreeWalker
	{
		double[] stack = new double[16]; // it's an example only!
		int top = -1;

		public void visit(ErrExpr expr)
		{
			stack[++top] = 0.0; // treat discarded expression as 0
		}
		
		public void visit(NumExpr expr)
		{
			stack[++top] = (double) expr.value;
//			System.out.println(stack[top]);
		}

		public void visit(NegExpr expr)
		{
			super.visit(expr);
			double v = stack[top--];
			stack[++top] = -v;
//			System.out.println("+/-");
//			System.out.println(stack[top]);
		}

		public void visit(MultExpr expr)
		{
			super.visit(expr);
			double r = stack[top--];
			double l = stack[top--];
			stack[++top] = l * r;
//			System.out.println("*");
//			System.out.println(stack[top]);
		}

		public void visit(DivExpr expr)
		{
			super.visit(expr);
			double r = stack[top--];
			double l = stack[top--];
			stack[++top] = l / r;
//			System.out.println("/");
//			System.out.println(stack[top]);
		}

		public void visit(PlusExpr expr)
		{
			super.visit(expr);
			double r = stack[top--];
			double l = stack[top--];
			stack[++top] = l + r;
//			System.out.println("+");
//			System.out.println(stack[top]);
		}

		public void visit(MinusExpr expr)
		{
			super.visit(expr);
			double r = stack[top--];
			double l = stack[top--];
			stack[++top] = l - r;
//			System.out.println("-");
//			System.out.println(stack[top]);
		}

		double getResult()
		{
			return stack[top];
		}
	}

	static public void main(String[] args) throws IOException, Parser.Exception
	{
		ExpressionParser parser = new ExpressionParser();
		ExpressionScanner input = new ExpressionScanner(new StringReader(args[0]));
		Expr expr = (Expr) parser.parse(input);
		Accumulator acc = new Accumulator();
		expr.accept(acc);
		System.out.println("= " + acc.getResult());
	}
}