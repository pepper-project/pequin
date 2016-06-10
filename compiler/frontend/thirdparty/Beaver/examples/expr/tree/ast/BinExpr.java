package expr.tree.ast;

public abstract class BinExpr extends Expr
{
	public final Expr l;
	public final Expr r;
	
	protected BinExpr(Expr left, Expr right)
	{
		super();
		l = left;
		r = right;
	}
}