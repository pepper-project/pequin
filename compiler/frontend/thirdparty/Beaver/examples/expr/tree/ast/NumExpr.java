package expr.tree.ast;

public class NumExpr extends Expr
{
	public final int value;
	
	public NumExpr(Number value)
	{
		super();
		this.value = value.intValue();
	}
	
	public void accept(TreeWalker walker)
	{
		walker.visit(this);
	}
}