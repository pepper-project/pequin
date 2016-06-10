package SFE.Compiler;

public class PointerType extends RestrictedUnsignedIntType{
  private Type base;

  public PointerType(Type base){
    super(64); //Assume 64 bit pointers.
    this.base = base;
  }
  
  public Type getPointedToType(){
    return base;
  }
}
