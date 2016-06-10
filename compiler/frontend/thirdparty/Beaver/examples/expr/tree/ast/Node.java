package expr.tree.ast;

import beaver.Symbol;

public abstract class Node extends Symbol
{
	public abstract void accept(TreeWalker walker); 
}