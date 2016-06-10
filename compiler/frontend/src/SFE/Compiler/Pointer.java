package SFE.Compiler;

import java.util.Collection;
import java.util.Collections;

import SFE.Compiler.Operators.ArrayAccessOperator;
import SFE.Compiler.Operators.StructAccessOperator;
import ccomp.CMemoryMap;

/**
 * An abstract implementation of a pointer.
 * 
 * A pointer holds a reference to an array of possible targets, incrementing a
 * pointer transforms it so that it points to one of its other targets.
 */
public class Pointer extends Expression implements Inlineable,
    OutputsToPolynomial {

	private int index;
	private int addressOfList;
	private int addressOfPointer;

	// list and array stores the variable this pointer points to.
	private LvalExpression[] list;
	private LvalExpression array;
	private PointerType myType;

	public Pointer(Type pointsToType, LvalExpression[] list, int addressOfList,
	    int addressOfPointer) {
		myType = new PointerType(pointsToType);
		this.list = list;
		this.addressOfList = addressOfList;
		this.addressOfPointer = addressOfPointer;
		this.index = addressOfPointer - addressOfList;
		if (index < 0 || index > maxIndex()) {
			throw new ArrayIndexOutOfBoundsException("Invalid pointer address.");
		}
	}

	/**
	 * This doesn't exactly work because a "heap" value will, through this
	 * function, also appear on the stack. This makes sense if you see the heap as
	 * part of the stack's address space, so that the heap appears multiple times
	 * in memory. (so every heap variable has an address on the stack, but some
	 * stack variables don't have an address in the heap, and all variables have
	 * distinct stack addresses)
	 */
	public static int getMemoryLocation(LvalExpression lval) {
		LvalExpression c = lval;
		// Arrays have address according to their first element.
		// Structs have address according to their first field.
		while (true) {
			if (c.getType() instanceof ArrayType) {
				c = new ArrayAccessOperator().resolve(c, IntConstant.ZERO);
			} else if (c.getType() instanceof StructType) {
				c = new StructAccessOperator(((StructType) c.getType()).getFields()
				    .firstElement()).resolve(c);
			} else {
				break;
			}
		}
		return c.getAddress();
//		return c.getOutputLine() + CMemoryMap.STACK;
	}

	private Pointer(LvalExpression array, int index) {
		this.list = null;

		this.array = array;
		this.addressOfList = getMemoryLocation(array);
		this.addressOfPointer = getMemoryLocation(new ArrayAccessOperator()
		    .resolve(array, IntConstant.valueOf(index)));
		this.index = index;
		ArrayType at = (ArrayType) array.getType();
		myType = new PointerType(at.getComponentType());
		if (index < 0 || index > maxIndex()) {
			throw new ArrayIndexOutOfBoundsException("Invalid pointer address.");
		}
	}

	public Pointer increment(IntConstant ic) {
		return increment(ic.toInt());
	}

	public Pointer increment(int i) {
		if (index + i < 0 || index + i > maxIndex()) {
			throw new ArrayIndexOutOfBoundsException("Invalid pointer indirection.");
		}
		if (list != null) {
			return new Pointer(myType.getPointedToType(), list, addressOfList,
			    addressOfPointer + i);
		} else {
			return new Pointer(array, index + i);
		}
	}

	private int maxIndex() {
		if (array == null) {
			return list.length;
		}
		return ((ArrayType) array.getType()).getLength();
	}

	public LvalExpression access() {
		if (list != null) {
			return list[index];
		}
		if (array != null) {
			return new ArrayAccessOperator().resolve(array,
			    IntConstant.valueOf(index));
		}
		return null;
	}

	public int size() {
		return 1; // A pointer can be held in a single field element
	}

	/**
	 * Duplicate the pointer across all of its bits.
	 */
	public Expression fieldEltAt(int i) {
		return this;
	}

	public Expression changeReference(VariableLUT unique) {
		if (list != null) {
			for (int i = 0; i < list.length; i++) {
				list[i] = list[i].changeReference(unique);
			}
		}
		if (array != null) {
			array = array.changeReference(unique);
		}
		return this;
	}

	public Collection<LvalExpression> getLvalExpressionInputs() {
		// A pointer stands alone - the data it points to is not strictly speaking
		// referenced by the pointer.
		return Collections.<LvalExpression> emptyList();
	}

	public PointerType getType() {
		return myType;
	}

	public IntConstant value() {
		return IntConstant.valueOf(addressOfPointer);
	}

	public static Pointer toPointerConstant(Expression c) {
		return toPointer(c);
	}

	// new statement might be generated, or we should try to inline
	public static Pointer toPointer(Expression c) {
		if (c instanceof Pointer) {
			return (Pointer) c;
		}
		if (c instanceof UnaryOpExpression) {
			UnaryOpExpression uo = ((UnaryOpExpression) c);
			// This handles *(&a)
			return toPointer(uo.getOperator().resolve(uo.getMiddle()));
		}
		if (c instanceof BinaryOpExpression) {
			BinaryOpExpression bo = ((BinaryOpExpression) c);
			// This handles *(a+i)
			return toPointer(bo.getOperator().resolve(bo.getLeft(), bo.getRight()));
		}
		// This handles *a
		if (c instanceof LvalExpression) {
			LvalExpression lvc = (LvalExpression) c;
			if (lvc.getType() instanceof ArrayType) {
				return new Pointer(lvc, 0);
			}
		}
		// this handles ... b = &a; c = *b
		AssignmentStatement as = AssignmentStatement.getAssignment(c);
		if (as != null) {
			for (Expression q : as.getAllRHS()) {
				Pointer got = toPointer(q);
				if (got != null) {
					return got;
				}
			}
		}
		// the pointer cannot be resolved at compile time as a constant pointer,
		// just return null.
		return null;
	}

	public Expression inline(Object obj, StatementBuffer history) {
		return this;
	}

	@Override
	public String toString() {
		return "&" + access();
	}
}
