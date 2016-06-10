package SFE.Compiler;

import java.util.HashMap;
import java.util.HashSet;

import SFE.Compiler.Operators.StructAccessOperator;

public class CacheManager {
    private static HashSet<Integer> notCached = new HashSet<Integer>();
    private static HashSet<Integer> shouldFlush = new HashSet<Integer>();
    private static HashMap<Integer, LvalExpression> memoryMapping = new HashMap<Integer, LvalExpression>();

    public static void setMemoryMapping(LvalExpression lvalue) {
        if (lvalue.getType() instanceof StructType) {
            StructType struct = (StructType) lvalue.getDeclaredType();
            for (int i = 0; i < struct.getFields().size(); i++) {
                String fieldname = struct.getFields().get(i);
                LvalExpression component = new StructAccessOperator(fieldname)
                    .resolve(lvalue);
                setMemoryMapping(component);
            }
        } else {
            // either array type or simple type.
            for (int i = 0; i < lvalue.size(); i++) {
                memoryMapping.put(lvalue.fieldEltAt(i).getAddress(),
                        lvalue.fieldEltAt(i));
            }
        }
    }

    public static void addCache(int address) {
        notCached.remove(address);
    }

    public static boolean isCached(int address) {
        if (notCached.contains(address)) {
            return false;
        } else {
            return true;
        }
    }

    // write cached variable at address back to RAM.
    public static Statement invalidateCache(int addr) {
        BlockStatement result = new BlockStatement();

        if (!notCached.contains(addr)) {
            LvalExpression lval = memoryMapping.get(addr);
            if (lval != null) {
                boolean doFlush = true;
                if (!shouldFlush.contains(addr)) {
                    // this is the first time we are flushing this variable
                    // all future times we must flush
                    shouldFlush.add(addr);
                    doFlush = false;

                    // now, do we force a flush this time?
                    // if the variable's value is 0, no; else, yes
                    final LvalExpression lvAssign = Function.getVars().getVar(lval.getName());
                    if (null != lvAssign) {
                        final StatementWithOutputLine lvAssignTmp = lvAssign.getAssigningStatement();
                        // if this is an AssignmentStatement, we can get its value
                        // otherwise it is something else, e.g., an InputStatement
                        // and we cannot assume it has value = 0.
                        if (lvAssignTmp instanceof AssignmentStatement) {
                            final AssignmentStatement lvAssignSt = (AssignmentStatement) lvAssign.getAssigningStatement();
                            if (null != lvAssignSt) {
                                // get the RHS of the assignment
                                final Expression[] assignRHS = lvAssignSt.getAllRHS();
                                // if this is a compound assignment, or not an integer constant, force a flush
                                if (assignRHS.length > 1 || ! (assignRHS[0] instanceof IntConstant || assignRHS[0] instanceof FloatConstant)) {
                                    doFlush = true;
                                } else {
                                    // this is a constant; make sure it's zero
                                    final FloatConstant assignConst = FloatConstant.toFloatConstant(assignRHS[0]);
                                    if (! assignConst.isZero()) {
                                        doFlush = true;
                                    }
                                }
                            }
                        } else {
                            doFlush = true;
                        }
                    }
                }

                if (doFlush) {
                    // the result is cached somewhere, generate ramput
                    RamPutEnhancedStatement ramput = new RamPutEnhancedStatement(
                            IntConstant.valueOf(addr), lval, IntConstant.ONE, true);
                    result.addStatement(ramput);
                }
                notCached.add(addr);
            } else {
                // throw new RuntimeException("Assertion failure.");
                System.out.println("WARNING: potential inaccurate point-to analysis.");
            }
        }

        return result;
    }
}
