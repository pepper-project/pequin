// UniqueVariables.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;


/**
 * A class that handles the unique variables.
 */
public class VariableLUT {	
  //~ Instance fields --------------------------------------------------------

  /*
   * link list of hashes holding variable scopes
   */
  private LinkedList vars;

  /*
   * Holds the parameters of all functions.
   */
  private Vector parameters;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructor
   */
  VariableLUT() {
    parameters     = new Vector();
    vars           = new LinkedList();
    pushScope();
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Adds a new scope (= new hash table in linked list)
   */
  public void pushScope() {
    vars.addFirst(new HashMap());
  }

  /**
   * Removes the current scope (= hash table in head of linked list)
   * @return HashMap the current poped scope
   */
  public Map popScope() {
    Map removeFirst = (Map) vars.removeFirst();
    return removeFirst;
  }

  /**
   * Returns all vars in current scope
   * @return Set all vars in current scope
   */
  public Set peekScope() {
    return ((Map) vars.getFirst()).keySet();
  }

  /*
   * Adds a parameter or local var to this function. Note that the function adds the LvalExpression for this parameter.
   *
   * Returns true if the new lvalue overwrites an existing lvalue in the same scope.
   * @param name the name of the new parameter/var
   * @param type the type of the new parameter/var
   * @param isParameter true is the new variable is paramenter.
   * @param isOutput true if it output variable
   */
  public LvalExpression addVar(String name, Type type, boolean isParameter,
                               boolean isOutput) {
    // create the lvalue of the given parameter type and name.
    Lvalue lvalue = new VarLvalue(new Variable(name, type), isOutput);

    //recursively expands things like multidimensional arrays, etc.
    Vector derivedLvalues = lvalue.getDerivedLvalues();

    for (int i = 0; i < derivedLvalues.size(); i++) {
      Lvalue lval = (Lvalue) (derivedLvalues.elementAt(i));
      add(lval, isParameter);
    }

    //I Think this is ok.
    return getVar(name);
  }

  /*
   * Adds a new parameter or variable.
   *
   * Returns true if the given name / parameter already exists.
   *
   * @param lval the Lvalue created for the var/parma.
   * @param isParameter true is the new variable is paramenter.
   */
  private void add(Lvalue lval, boolean isParameter) {
    String         name    = lval.getName();

    LvalExpression lvalExp = new LvalExpression(lval);

    // add to local vars - current scope
    if (((Map) vars.getFirst()).put(name, lvalExp) != null) {
      //System.out.println("Overwrote reference with name "+name);
    } else {
      //New variable, at least in this scope
      //System.out.println(name);
    }
    //System.out.println(((Map)vars.getFirst()).size());

    // add to parameters - if needed
    if (isParameter
        //Remove placeholder variables, i.e. indeterminate array variables and whole-struct / whole-array vars
        && lvalExp.getLvalue().isPrimitive() && !name.matches(".*\\[\\$\\].*")) {
      parameters.add(lvalExp);
    }
  }

  /**
   * Returns the LvalExpression reference of a var.
   * It searches the scopes from the current scope till the global scope.
   * @return LvalExpression the reference of the variable
   */
  public LvalExpression getVar(String name) {
    ListIterator iterator = vars.listIterator(0);

    while (iterator.hasNext()) {
      Map scope = (Map) (iterator.next());

      if (scope.containsKey(name)) {
        return (LvalExpression) scope.get(name);
      }
    }

    return null;
  }

  /**
   * Returns the scope containing the LvalExpression reference of a var.
   * It searches the scopes from the current scope till the global scope.
   * @return LvalExpression the reference of the variable
   */
  public Map getVarScope(String name) {
    ListIterator iterator = vars.listIterator(0);

    while (iterator.hasNext()) {
      Map scope = (Map) (iterator.next());

      if (scope.containsKey(name)) {
        return scope;
      }
    }

    return null;
  }


  /*
   * Replace all variables and parameters LvalExpressions
   * with primitive type LValExpressions
   */
  public void toSLPTCircuit() {
    //This does nothing.
    
    /*
    Lvalue oldLvalue;
    String oldName;

    // hold old params and locals
    Vector<LvalExpression> oldParameters = parameters;
    Collection<LvalExpression> oldVars = popScope().values();

    // create new params vector and loacls scope
    parameters = new Vector();
    pushScope();

    for(LvalExpression oldVar : oldVars) {
      // get the old LvalExpression's Lvalue
      oldLvalue = oldVar.getLvalue();

      //don't lose the old multibit var
      //add(oldLvalue, isParameter);
      add(oldLvalue, false);

      if (oldLvalue.hasDerives()) {
        continue;
      }

      // insert new single bit variables
      for (int i = 0; i < oldLvalue.size(); i++) {
        Lvalue bitLvalue = new BitLvalue(oldLvalue, i);
        //System.out.println(BitLvalue);
        add(bitLvalue, false);
      }
    }
    oldVars.clear();

    for(LvalExpression oldVar : oldParameters) {
      oldLvalue = oldVar.getLvalue();

      //parameters.add(oldVar);

      if (oldLvalue.hasDerives()) {
        continue;
      }

      for (int i = 0; i < oldLvalue.size(); i++) {
        Lvalue bit = new BitLvalue(oldLvalue, i);
        parameters.add(getVar(bit.getName()));
        //parameters.add((LvalExpression)((Map)vars.getFirst()).get(bit.getName()));
      }
    }

    oldParameters.clear();
    */
  }

  /*
   * Returns the vector of the parametes.
   */
  public Vector getParameters() {
    return parameters;
  }

  //Static functions
  /*
  public static Collection<LvalExpression> uniqueLvals(Collection<LvalExpression> lvals) {
  	TreeMap<Integer, LvalExpression> unique = new TreeMap();
  	for(LvalExpression lv : lvals){
  		unique.put(new Integer(lv.toString()), lv);
  	}
  	return unique.values();
  }
  */


  public final static HashMap<String, String> STRING_CACHE = new HashMap();
}
