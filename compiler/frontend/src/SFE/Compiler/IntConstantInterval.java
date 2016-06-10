package SFE.Compiler;

/**
 * A closed interval over Z.
 */
public class IntConstantInterval {
  public final IntConstant upper;
  public final IntConstant lower;

  public IntConstantInterval(IntConstant lower, IntConstant upper) {
    this.lower = lower;
    this.upper = upper;
    if (lower.compareTo(upper) > 0){
      throw new RuntimeException("Assertion error: empty interval.");
    }
  }

  public IntConstantInterval addNtimes(int m) {
    //The smallest value is m*lower, the biggest value is m*upper.
    IntConstant m_ = IntConstant.valueOf(m);
    return new IntConstantInterval(lower.multiply(m_), upper.multiply(m_));
  }
  public IntConstantInterval add(IntConstantInterval other) {
    //The smallest value is the sum of the lowers, the biggest is the sum of the biggers.
    return new IntConstantInterval(lower.add(other.lower), upper.add(other.upper));
  }
  public IntConstantInterval multiply(IntConstantInterval other) {
    //More complicated!
    IntConstant nlow = null;
    IntConstant nup = null;
    for(IntConstant a : new IntConstant[]{lower, upper}){
      for(IntConstant b : new IntConstant[]{other.lower, other.upper}){
        IntConstant prod = a.multiply(b);
        if (nlow == null || prod.compareTo(nlow) < 0){
          nlow = prod;
        }
        if (nup == null || prod.compareTo(nup) > 0){
          nup = prod;
        }
      }
    }
    return new IntConstantInterval(nlow, nup);
  }

  public boolean isSubInterval(IntConstantInterval other) {
    return lower.compareTo(other.lower) >= 0 && upper.compareTo(other.upper) <= 0;
  }

  public IntConstantInterval looseUnion(IntConstantInterval other) {
    IntConstant ulower = lower.compareTo(other.lower) < 0 ? lower : other.lower;
    IntConstant uupper = upper.compareTo(other.upper) > 0 ? upper : other.upper;
    return new IntConstantInterval(ulower, uupper);
  }

  public IntConstantInterval looseIntersect(IntConstantInterval other) {
    IntConstant ulower = lower.compareTo(other.lower) > 0 ? lower : other.lower;
    IntConstant uupper = upper.compareTo(other.upper) < 0 ? upper : other.upper;
    return new IntConstantInterval(ulower, uupper);
  }
}
