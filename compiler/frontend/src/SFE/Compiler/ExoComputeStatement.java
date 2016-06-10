package SFE.Compiler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import ccomp.CBuiltinFunctions;

public class ExoComputeStatement extends StatementWithOutputLine implements OutputWriter {
    // each list in inVars corresponds to one of the arrays in inLs
    private final List<List<LvalExpression>> inVars;
    // each list in outVars corresponds to one of the structs in outStrP;
    private final List<LvalExpression> outVars;
    private final int compNum;
    private int outputLine = -1;

    // circuit writer for use by some IterLLCalls
    private PrintWriter circuit;

    public ExoComputeStatement(List<List<LvalExpression>> inVars, List<LvalExpression> outVars, int compNum) {
        this.inVars = inVars;
        this.outVars = outVars;
        this.compNum = compNum;
    }

    public Statement duplicate() {
        return new ExoComputeStatement(inVars, outVars, compNum);
    }

    public Statement toSLPTCircuit(Object obj) {
        // resolve input and output variables to their corresponding field element
        iterLL(inVars,circuitIter);
        iterL(outVars,circuitIter);
        return this;
    }

    public void toAssignmentStatements(StatementBuffer assignments) {
        // change references
        iterLL(inVars,changeRefIter);
        iterL(outVars,changeRefIter);

        toAssignmentStatements_NoChangeRef(assignments);
    }

    public void toAssignmentStatements_NoChangeRef(StatementBuffer assignments) {
        // make sure variables have references
        iterLL(inVars,addRefIter);
        iterL(outVars,addRefIter);

        outputLine = Program.getLineNumber();
        assignments.add(this);
    }

    public int getOutputLine() {
        return outputLine;  // returns -1 if not yet assigned
    }

    public void toCircuit(Object obj, PrintWriter circuit) {
        this.circuit = circuit;

        // exo_compute
        circuit.print(getOutputLine() + " " + CBuiltinFunctions.EXO_COMPUTE_NAME +
                      " EXOID " + Integer.toString(compNum) + " INPUTS [");

        // input variables
        iterLL(inVars,toCircuitIter,toCBrkIter);
        circuit.print("] ] OUTPUTS [ ");

        // output variables
        iterL(outVars,toCircuitIter);
        circuit.print("]");

        circuit.println("\t// " + this.toString());
    }

    public String toString() {
        return "exo_compute #" + Integer.toString(compNum) +
               " inVectors=" + inVars.size() +
               " outVars=" + outVars.size();
    }


// ** iterator infrastructure for input and output variables **
    // iterate over the list of lists, doing... something.
    private void iterL(List<LvalExpression> thisL, IterLLCall illfn) {
        for (int i=0; i<thisL.size(); i++) {
            illfn.call(thisL,i);
        }
    }
    private void iterLL(List<List<LvalExpression>> lls, IterLLCall illfn, IterLLCall...repfns) {
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
    private abstract class IterLLCall { abstract void call (List<LvalExpression> thisL, int i); }

    // get the field element in the toSLPTCircuit pass
    private IterLLCall circuitIter = new IterLLCall() {
        void call (List<LvalExpression> thisL, int i) { thisL.set(i,thisL.get(i).fieldEltAt(0)); }
    };

    // change reference in the toAssignmentStatements pass
    private IterLLCall changeRefIter = new IterLLCall() {
        void call (List<LvalExpression> thisL, int i) { thisL.set(i,thisL.get(i).changeReference(Function.getVars())); }
    };

    // reference for each variable during the toAssignmentStatements pass
    private IterLLCall addRefIter = new IterLLCall() {
        void call (List<LvalExpression> thisL, int i) { thisL.get(i).addReference(); }
    };

    // call toCircuit on each LvalExpression
    private IterLLCall toCircuitIter = new IterLLCall() {
        void call (List<LvalExpression> thisL, int i) {
            thisL.get(i).toCircuit(null, ExoComputeStatement.this.circuit);
            ExoComputeStatement.this.circuit.print(" ");
        }
    };

    // bracketing utility
    private IterLLCall toCBrkIter = new IterLLCall() {
        void call (List<LvalExpression> thisL, int j) {
            // close the previous bracket unless we're on the first iteration
            if (0 != j) {
                ExoComputeStatement.this.circuit.print("]");
            }

            ExoComputeStatement.this.circuit.print(" [ ");
        }
    };
}
