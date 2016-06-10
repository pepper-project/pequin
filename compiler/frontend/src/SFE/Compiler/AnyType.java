package SFE.Compiler;

/**
 * Represents a primitive type.
 */
public class AnyType extends Type {
  public int size() {
    throw new RuntimeException("AnyType has indeterminate representation");
  }

  public String fieldEltAt(String rootName, int bit) {
    throw new RuntimeException("AnyType has indeterminate representation");
  }

  public Type fieldEltTypeAt(int bit) {
    throw new RuntimeException("AnyType has indeterminate representation");
  }
}
