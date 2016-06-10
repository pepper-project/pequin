// Consts.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Vector;


/**
 * The Consts class stores the constants defeined in the program.
 */
public class Consts {

  //~ Methods ----------------------------------------------------------------

  /**
   * Returns a ConstExpression representing the const of the specified type name, or null if there was no
   * such type defined for this type name.
   * @param name the name of the constant whose associated ConstExpression is to be returned.
   * @return ConstExpression representing the const, or null if there was no such constant defined.
   */
  public static ConstExpression fromName(String name) {
    return ((ConstExpression) (constsTable.get(name)));
  }

  /**
   * Associates the specified new constant name with the specified integer constant.
   * @param newConstName the new constant name with which the specified constant is to be associated.
   * @param constant the constant to be associated with the specified newConstName.
   * @throws IllegalArgumentException if the newConstName is already defined.
   * 
   * If allow_duplicate is true, overwrite the existing constant with this name without throwing an exception
   */
  public static void addConst(String name, final ConstExpression value, boolean allow_duplicate) throws IllegalArgumentException {
    if (!allow_duplicate){
      if (fromName(name) != null) {
        throw new RuntimeException("Constant defined twice: "+name);
      }
    }
    declaredConsts.put(name, value);
    
    constsTable.put(name, value);
    if (value instanceof ArrayConstant){
      constsTable.put(name+"[$]", value);
    }
  }

  /**
   * Returns a list of all constExpressions which have been passed to Consts through defineName().
   *
   * Does not return all of the derived constants produced as a result of those additions.
   */
  public static Collection<Entry<String, ConstExpression>> getDeclaredConsts() {
    return declaredConsts.entrySet();
  }


  //~ Static fields

  /*
   * holds the constants defined in the program (slow to lookup because not hashmap, but sorted output)
   */
  private static Map<String, ConstExpression> declaredConsts = new TreeMap();
  
  /*
   * Hashmap version of declaredConsts (faster lookup by name), but also includes derived constants
   * 
   * i.e. array constant Fn has derived Fn[$] and Fn[0] etc...
   */
  private static HashMap<String, ConstExpression> constsTable = new HashMap();
}
