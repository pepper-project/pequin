package SFE.Compiler;

import java.util.Vector;

/**
 * A set of utility methods for comparing and intersecting types
 */
public class TypeHeirarchy {

  /**
   * @returns isSubType(left, right) && isSubType(right, left);
   */
  public static boolean equals(Type left, Type right) {
    return isSubType(left, right) && isSubType(right, left);
  }
  /**
   * Returns true if a is a subtype of b
   *
   * Throws a ClassCastException if the result cannot be determined.
   */
  public static boolean isSubType(Type a, Type b) {
    if (a instanceof RestrictedIntType){
      RestrictedIntType a_ = (RestrictedIntType)a;
      if (b instanceof RestrictedIntType){
        RestrictedIntType b_ = (RestrictedIntType)b;
        return a_.getInterval().isSubInterval(b_.getInterval());
      }
    }
    
    if (b instanceof AnyType) {
      return true;
    }

    if (b instanceof BusType) {
      BusType bus = (BusType)b;
      if (a instanceof FiniteType) {
        if (a instanceof FloatType || a instanceof IntType) {
          return true;
        }
      }
    }
    
    if (a instanceof StructType){
      Vector<Type> aFields = ((StructType)a).getFieldTypes();
      if (!(b instanceof StructType)){
        return false;
      }
      Vector<Type> bFields = ((StructType)b).getFieldTypes();
      if (aFields.size() != bFields.size()){
        return false;
      }
      for(int i = 0; i < aFields.size(); i++){
        if (!isSubType(aFields.get(i), bFields.get(i))){
          return false;
        }
      }
      return true;
    }
    

    if (a instanceof ArrayType){
      Type aBase = ((ArrayType)a).getComponentType();
      if (!(b instanceof ArrayType)){
        return false;
      }
      Type bBase = ((ArrayType)b).getComponentType();
      return isSubType(aBase, bBase);
    }
    

    if (a instanceof RestrictedUnsignedIntType) {
      RestrictedUnsignedIntType ra = (RestrictedUnsignedIntType)a; //at minimum {0, 1}
      if (b instanceof RestrictedUnsignedIntType) {
        RestrictedUnsignedIntType rb = (RestrictedUnsignedIntType)b; //at minimum {0,1}
        return ra.getLength() <= rb.getLength();
      } else if (b instanceof RestrictedSignedIntType) {
        RestrictedSignedIntType rb = (RestrictedSignedIntType)b; //at minimum {-1, 0}
        return ra.getLength() <= rb.getLength() - 1;
      } else if (b instanceof RestrictedFloatType) {
        RestrictedFloatType rb = (RestrictedFloatType)b; //at minimum {-1, -1/2, 0, 1/2, 1}
        return ra.getLength() <= rb.getNa();
      } else if (b instanceof IntType) {
        return true;
      } else if (b instanceof FloatType) {
        return true;
      }
    } else if (a instanceof RestrictedSignedIntType) {
      RestrictedSignedIntType ra = (RestrictedSignedIntType)a; //at minimum {-1, 0}
      //values in ra = {-2^(n-1), -2^(n-1) + 1, ... 2^(n-1) - 1}.
      if (b instanceof RestrictedUnsignedIntType) {
        return false;
      } else if (b instanceof RestrictedSignedIntType) {
        RestrictedSignedIntType rb = (RestrictedSignedIntType)b;
        return ra.getLength() <= rb.getLength();
      } else if (b instanceof RestrictedFloatType) {
        RestrictedFloatType rb = (RestrictedFloatType)b; //at minimum {-1, -1/2, 0, 1/2, 1}
        //integers in rb go from -2^rb.getNa() + 1 to 2^rb.getNa() - 1
        //so 2, 1 fails, but 2, 2 succeeds:
        return ra.getLength() <= rb.getNa();
      }
      //Infinite types
      else if (b instanceof IntType) {
        return true;
      } else if (b instanceof FloatType) {
        return true;
      }
    } else if (a instanceof RestrictedFloatType) {
      RestrictedFloatType ra = (RestrictedFloatType)a; //at minimum {-1, -1/2, 0, 1/2, 1}
      //Floats with pot denominators, with numerator absolute value under 2^na and denominator between 1 and 2^nb, inclusive.
      if (b instanceof IntType) {
        return false;
      } else if (b instanceof RestrictedFloatType) {
        RestrictedFloatType rb = (RestrictedFloatType)b; //at minimum {-1, 0, 1}
        //integers in rb go from -2^rb.getNa() + 1 to 2^rb.getNa() - 1
        //so 2, 1 fails, but 2, 2 succeeds:
        return ra.getNa() <= rb.getNa() && ra.getNb() <= rb.getNb();
      }
      //Infinite types
      else if (b instanceof FloatType) {
        return true;
      }
    } else if (a instanceof IntType) {
      //infinite ints
      if (b instanceof FiniteType) {
        return false;
      } else if (b instanceof IntType) {
        return true; //for restricted int, minimum set is {-2, -1, 0, \1}.
      } else if (b instanceof FloatType) {
        return true; //for restricted float, minimum set is {-1, 0, 1}.
      }
    } else if (a instanceof FloatType) {
      //infinite floats
      if (b instanceof FiniteType) {
        return false;
      } else if (b instanceof IntType) {
        return false;
      } else if (b instanceof FloatType) {
        return true; //for restricted float, minimum set is {-1, 0, 1}.
      }
    }

    //throw new ClassCastException("Cannot determine whether "+a+" is a subtype of "+b);
    return false;
  }

  /**
   * Returns a type that contains the intersection of the types a and b.
   *
   * The implementation will attempt to return a type that is not too much bigger than
   * the intersection of a and b.
   *
   * Guarantees:
   * If a is a subtype of b, a is returned
   * If b is a subtype of a, b is returned
   *
   */
  public static Type looseIntersect(Type a, Type b) {
    if (isSubType(a, b)) {
      return a;
    }
    if (isSubType(b, a)) {
      return b;
    }
    //So b is not a subtype of a and a is not a subtype of b

    //A few special cases we handle.
    if (a instanceof RestrictedUnsignedIntType) {
      RestrictedUnsignedIntType ra = (RestrictedUnsignedIntType)a; //at minimum {0, 1}
      if (b instanceof RestrictedUnsignedIntType) {
        RestrictedUnsignedIntType rb = (RestrictedUnsignedIntType)b; //at minimum {0,1}
        return new RestrictedUnsignedIntType(ra.getInterval().looseIntersect(rb.getInterval()));
      } else if (b instanceof RestrictedSignedIntType) {
        RestrictedSignedIntType rb = (RestrictedSignedIntType)b; //at minimum {-1, 0}
        return new RestrictedUnsignedIntType(ra.getInterval().looseIntersect(rb.getInterval()));
      } else if (b instanceof RestrictedFloatType) {
        RestrictedFloatType rb = (RestrictedFloatType)b; //at minimum {-1, -1/2, 0, 1/2, 1}
        return new RestrictedUnsignedIntType(Math.min(ra.getLength(), rb.getNa()));
      }
    } else if (a instanceof RestrictedSignedIntType) {
      RestrictedSignedIntType ra = (RestrictedSignedIntType)a; //at minimum {-1, 0}
      //values in ra = {-2^(n-1), -2^(n-1) + 1, ... 2^(n-1) - 1}.
      if (b instanceof RestrictedUnsignedIntType) {
        RestrictedUnsignedIntType rb = (RestrictedUnsignedIntType)b; //at minimum {0,1}
        return new RestrictedUnsignedIntType(ra.getInterval().looseIntersect(rb.getInterval()));
      } else if (b instanceof RestrictedSignedIntType) {
        RestrictedSignedIntType rb = (RestrictedSignedIntType)b;
        return new RestrictedSignedIntType(ra.getInterval().looseIntersect(rb.getInterval()));
      } else if (b instanceof RestrictedFloatType) {
        RestrictedFloatType rb = (RestrictedFloatType)b; //at minimum {-1, -1/2, 0, 1/2, 1}
        //integers in rb go from -2^rb.getNa() + 1 to 2^rb.getNa() - 1
        //so 2, 1 fails, but 2, 2 succeeds:
        return new RestrictedSignedIntType(Math.min(ra.getLength(), rb.getNa()+1));
      }
    } else if (a instanceof RestrictedFloatType) {
      RestrictedFloatType ra = (RestrictedFloatType)a; //at minimum {-1, -1/2, 0, 1/2, 1}
      if (b instanceof RestrictedUnsignedIntType) {
        RestrictedUnsignedIntType rb = (RestrictedUnsignedIntType)b; //at minimum {0,1}
        return new RestrictedUnsignedIntType(Math.min(ra.getNa(), rb.getLength()));
      } else if (b instanceof RestrictedSignedIntType) {
        RestrictedSignedIntType rb = (RestrictedSignedIntType)b; //at minimum {-1,0}
        return new RestrictedSignedIntType(Math.min(ra.getNa()+1, rb.getLength()));
      } else if (b instanceof RestrictedFloatType) {
        RestrictedFloatType rb = (RestrictedFloatType)b; //at minimum {-1, -1/2, 0, 1/2, 1}
        //integers in rb go from -2^rb.getNa() + 1 to 2^rb.getNa() - 1
        //so 2, 1 fails, but 2, 2 succeeds:
        return new RestrictedFloatType(Math.min(ra.getNa(), rb.getNa()), Math.min(ra.getNb(), rb.getNb()));
      }
    }

    throw new ClassCastException("Cannot intersect types "+a+" and "+b);
  }

  /**
   * Returns a type that contains both types a and b.
   *
   * The implementation will attempt to choose such a type that is not too much "bigger than" a and b,
   * making use of the available types.
   *
   * Guarantees:
   * If a is a subtype of b, b is returned
   * If b is a subtype of a, a is returned
   */
  public static Type looseUnion(Type a, Type b) {
    if (isSubType(a, b)) {
      return b;
    }
    if (isSubType(b, a)) {
      return a;
    }
    //So b is not a subtype of a and a is not a subtype of b

    //A few special cases we handle.
    if (a instanceof RestrictedUnsignedIntType) {
      RestrictedUnsignedIntType ra = (RestrictedUnsignedIntType)a; //at minimum {0, 1}
      if (b instanceof RestrictedUnsignedIntType) {
        RestrictedUnsignedIntType rb = (RestrictedUnsignedIntType)b; //at minimum {0,1}
        return new RestrictedUnsignedIntType(ra.getInterval().looseUnion(rb.getInterval()));
      } else if (b instanceof RestrictedSignedIntType) {
        RestrictedSignedIntType rb = (RestrictedSignedIntType)b; //at minimum {-1, 0}
        return new RestrictedSignedIntType(ra.getInterval().looseUnion(rb.getInterval()));
      } else if (b instanceof RestrictedFloatType) {
        RestrictedFloatType rb = (RestrictedFloatType)b; //at minimum {-1, -1/2, 0, 1/2, 1}
        return new RestrictedFloatType(Math.max(ra.getLength(), rb.getNa()), rb.getNb());
      }
    } else if (a instanceof RestrictedSignedIntType) {
      RestrictedSignedIntType ra = (RestrictedSignedIntType)a; //at minimum {-1, 0}
      //values in ra = {-2^(n-1), -2^(n-1) + 1, ... 2^(n-1) - 1}.
      if (b instanceof RestrictedUnsignedIntType) {
        RestrictedUnsignedIntType rb = (RestrictedUnsignedIntType)b; //at minimum {0,1}
        return new RestrictedSignedIntType(ra.getInterval().looseUnion(rb.getInterval()));
      } else if (b instanceof RestrictedSignedIntType) {
        RestrictedSignedIntType rb = (RestrictedSignedIntType)b;
        return new RestrictedSignedIntType(ra.getInterval().looseUnion(rb.getInterval()));
      } else if (b instanceof RestrictedFloatType) {
        RestrictedFloatType rb = (RestrictedFloatType)b; //at minimum {-1, -1/2, 0, 1/2, 1}
        //integers in rb go from -2^rb.getNa() + 1 to 2^rb.getNa() - 1
        //so 2, 1 fails, but 2, 2 succeeds:
        return new RestrictedFloatType(Math.max(ra.getLength(), rb.getNa()), rb.getNb());
      }
    } else if (a instanceof RestrictedFloatType) {
      RestrictedFloatType ra = (RestrictedFloatType)a; //at minimum {-1, -1/2, 0, 1/2, 1}
      if (b instanceof RestrictedUnsignedIntType) {
        RestrictedUnsignedIntType rb = (RestrictedUnsignedIntType)b; //at minimum {0,1}
        return new RestrictedFloatType(Math.max(ra.getNa(), rb.getLength()), ra.getNb());
      } else if (b instanceof RestrictedSignedIntType) {
        RestrictedSignedIntType rb = (RestrictedSignedIntType)b; //at minimum {-1,0}
        return new RestrictedFloatType(Math.max(ra.getNa(), rb.getLength()), ra.getNb());
      } else if (b instanceof RestrictedFloatType) {
        RestrictedFloatType rb = (RestrictedFloatType)b; //at minimum {-1, -1/2, 0, 1/2, 1}
        //integers in rb go from -2^rb.getNa() + 1 to 2^rb.getNa() - 1
        //so 2, 1 fails, but 2, 2 succeeds:
        return new RestrictedFloatType(Math.max(ra.getNa(), rb.getNa()), Math.max(ra.getNb(), rb.getNb()));
      }
    }

    throw new ClassCastException("Cannot union types "+a+" and "+b);

    /*

    if (isSubType(a, b)){
    	return b;
    }
    if (isSubType(b, a)){
    	return a;
    }
    if (a instanceof RestrictedSignedIntType && b instanceof RestrictedUnsignedIntType){
    	return new RestrictedSignedIntType(
    			Math.max(((RestrictedSignedIntType)a).getLength(),
    					((RestrictedUnsignedIntType)b).getLength() + 1));
    }

    if (a instanceof RestrictedUnsignedIntType && b instanceof RestrictedSignedIntType){
    	return new RestrictedSignedIntType(
    			Math.max(((RestrictedUnsignedIntType)a).getLength() + 1,
    					((RestrictedSignedIntType)b).getLength()));
    }

    */
  }


}
