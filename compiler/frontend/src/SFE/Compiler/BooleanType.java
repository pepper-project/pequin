// BooleanType.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;


/**
 * A class representing the boolean primitive type
 * that can be defined in the program.
 *
 * These are represented as the integers 0 (false) or 1 (true)
 */
public class BooleanType extends RestrictedUnsignedIntType {
  public BooleanType() {
    super(1);
  }
}
