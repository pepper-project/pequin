package expr.tree.ast;

public class ErrExpr extends Expr
{
    
	public ErrExpr()
	{
		super();
	}
	
	public void accept(TreeWalker walker)
	{
		walker.visit(this);
	}
}