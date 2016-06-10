package SFE.Compiler;

/**
 * Floats with pot denominators, with numerator absolute value under 2^na and denominator between 1 and 2^nb, inclusive.
 *
 * Note that when na = 1 and nb = 0 (the minimum case), this describes the set {-1, -1/2, 0, 1/2, 1}.
 */
public class RestrictedFloatType extends FloatType implements FiniteType {
  public RestrictedFloatType(int na, int nb) {
    this.na = na;
    this.nb = nb;
    if (na <= 0 || nb <= 0) {
      throw new RuntimeException("Nonpositive na, nb in restricted float type definition");
    }
  }
  private int na, nb;
  public String toString() {
    return "float n_a "+na+" n_b " + nb;
  }

  public int getNa() {
    return na;
  }
  public int getNb() {
    return nb;
  }
}
