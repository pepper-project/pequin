package expr.tree.ast;

public class NegExpr extends Expr
{
	public final Expr e;
	
	public NegExpr(Expr expr)
	{
		super();
		this.e = expr;
	}
	
	public void accept(TreeWalker walker)
	{
		walker.visit(this);
	}
}