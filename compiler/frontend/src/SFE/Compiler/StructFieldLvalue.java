// StructFieldLvalu.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;


/**
 * The StructFieldLvalue class extends the VarLvalue class, and represents
 * a struct field inside an Lvalue with struct type
 */
public class StructFieldLvalue extends VarLvalue {
  //~ Instance fields --------------------------------------------------------

  //data members

  /*
   * The base Lvalue (lvalue has StructType type)
   */
  private Lvalue base;

  /*
   * The name of the field in the base's Struct that this
   * StructFieldLvalue represents.
   */
  //private String field;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a new StructFieldLvalue from a given lvalue and fild name.
   * @param base the type of the field
   * @param field field name
   */
  public StructFieldLvalue(Lvalue base, String field) {
    super(new Variable(base.getName()+"."+field,((StructType)base.getType()).fromFieldName(field)), base.isOutput());
    this.base         = base;
    //this.field        = field;
  }
}
