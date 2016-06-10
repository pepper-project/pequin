package expr.tree.ast;

public class PlusExpr extends BinExpr
{
	public PlusExpr(Expr left, Expr right)
	{
		super(left, right);
	}
	
	public void accept(TreeWalker walker)
	{
		walker.visit(this);
	}
}