package SFE.Compiler;

/**
 * Implemented by expressions which can be inlined
 */
public interface Inlineable {
  /*
  public static enum InliningConstraints {
  	INLINE_RHS, //Inline without increasing computation cost, while still returning a valid RHS expression
  	INLINE_POLYNOMIAL, //Inline without increasing computation cost, while still returning an expression coercible to a polynomial
  	INLINE_POLYNOMIAL_D1, //Inline without increasing computation cost, while still returning an expression coercible to a degree 1 polynomial
  	INLINE_POLYNOMIAL_D2, //Inline without increasing computation cost, while still returning an expression coercible to a degree 2 polynomial
  	INLINE_LVAL_CONST, //Inline without increasing computation cost, only return LVals or const.
  	INLINE_MUX, //Inline any mux representation of this expression
  }
  */
  /**
   * Parameter obj is a context parameter.
   */
  public Expression inline(Object obj, StatementBuffer history);
}
