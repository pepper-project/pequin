package SFE.Compiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * An array of constants, hence itself a constant
 *
 * As elements are added, the type of this constant changes (but the base type doesn't change).
 */
public class ArrayConstant extends ConstExpression {
  private List<ConstExpression> values;
  private Type baseType;

  public ArrayConstant(Type baseType) {
    this.baseType = baseType;
    values = new ArrayList();
  }

  /**
   * Type checks.
   */
  public void add(ConstExpression c) {
    if (!TypeHeirarchy.isSubType(c.getType(), baseType)) {
      throw new RuntimeException("Type error: Cannot add object "+c+" to list of type "+baseType);
    }
    values.add(c);
  }

  public ConstExpression get(int i) {
    return values.get(i);
  }

  public List<ConstExpression> getValues() {
    return values;
  }

  public ArrayType getType() {
    return new ArrayType(baseType, size());
  }

  public int size() {
    return values.size();
  }
}
