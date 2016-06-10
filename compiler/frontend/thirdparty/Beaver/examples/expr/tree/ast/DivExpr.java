package expr.tree.ast;

public class DivExpr extends BinExpr
{
	public DivExpr(Expr left, Expr right)
	{
		super(left, right);
	}
	
	public void accept(TreeWalker walker)
	{
		walker.visit(this);
	}
}