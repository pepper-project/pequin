package SFE.Compiler;

public class RestrictedIntType extends IntType implements FiniteType{
  private IntConstantInterval interval;

  public RestrictedIntType(IntConstantInterval interval){
    this.interval = interval;
  }

  public IntConstantInterval getInterval() {
    return interval;
  }
}
