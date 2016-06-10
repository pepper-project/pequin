package SFE.Compiler;

/**
 * Convenience implementation of methods for types that occupy a single
 * field element.
 */
public abstract class ScalarType extends Type{
  public final int size() {
    return 1;
  }

  public final String fieldEltAt(String rootName, int bit) {
    if (bit < 0 || bit > size()){
      throw new RuntimeException("Array index out of bounds exception, fieldEltAt index "+bit+" scalar type "+rootName);
    }
    return rootName;
  }

  public final Type fieldEltTypeAt(int bit) {
    if (bit < 0 || bit > size()){
      throw new RuntimeException("Array index out of bounds exception, fieldEltTypeAt index "+bit+" scalar type "+this);
    }
    return this;
  }
}
