package SFE.Compiler.util;

import java.io.PrintWriter;
import java.util.List;

import SFE.Compiler.LvalExpression;
import SFE.Compiler.Function;

/**
 * A collection of utility methods shared by multiple statements.
 */
public class ExpressionIterator {
    // ** iterator infrastructure for input and output variables **
    // iterate over the list of lists, doing... something.
    public static void iterL(List<LvalExpression> thisL, IterLLCall illfn) {
        for (int i=0; i<thisL.size(); i++) {
            illfn.call(thisL,i);
        }
    }
    public static void iterLL(List<List<LvalExpression>> lls, IterLLCall illfn, IterLLCall...repfns) {
        int j=0;
        for (List<LvalExpression> thisL : lls) {
            // pre-walk call
            if (repfns.length > 0) {
                repfns[0].call(thisL,j++);
            }

            // per-element iter call
            iterL(thisL,illfn);
        }
    }

    // lack of lambdas brings us to this syntactic noise. Oh well.
    public abstract class IterLLCall { abstract void call (List<LvalExpression> thisL, int i); }
    protected final IterLLCall circuitIter = new IterLLCall() {
        void call (List<LvalExpression> thisL, int i) { thisL.set(i,thisL.get(i).fieldEltAt(0)); }
    };
    protected final IterLLCall changeRefIter = new IterLLCall() {
        void call (List<LvalExpression> thisL, int i) { thisL.set(i,thisL.get(i).changeReference(Function.getVars())); }
    };
    protected final IterLLCall addRefIter = new IterLLCall() {
        void call (List<LvalExpression> thisL, int i) { thisL.get(i).addReference(); }
    };
    protected IterLLCall _toCircuitIter(final PrintWriter circuit) {
        return new IterLLCall() {
            void call (List<LvalExpression> thisL, int i) {
                thisL.get(i).toCircuit(null, circuit);
                circuit.print(" ");
            }
        };
    }
    protected IterLLCall _toCBrkIter(final PrintWriter circuit) {
        return new IterLLCall() {
            void call (List<LvalExpression> thisL, int j) {
                // close the previous bracket unless we're on the first iteration
                if (0 != j) {
                    circuit.print("]");
                }
                circuit.print(" [ ");
            }
        };
    }

    private static final ExpressionIterator instance = new ExpressionIterator();

    // get the field element in the toSLPTCircuit pass
    public static IterLLCall circuitIter() {
        return instance.circuitIter;
    }

    // change reference in the toAssignmentStatements pass
    public static IterLLCall changeRefIter() {
        return instance.changeRefIter;
    }

    // reference for each variable during the toAssignmentStatements pass
    public static IterLLCall addRefIter() {
        return instance.addRefIter;
    }

    // call toCircuit on each LvalExpression
    public static IterLLCall toCircuitIter(PrintWriter circuit) {
        return instance._toCircuitIter(circuit);
    }

    // bracketing utility
    public static IterLLCall toCBrkIter(PrintWriter circuit) {
        return instance._toCBrkIter(circuit);
    }
}