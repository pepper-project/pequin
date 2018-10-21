package SFE.Compiler;

import java.io.PrintWriter;
import java.util.List;

import SFE.Compiler.util.ExpressionIterator;
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
        ExpressionIterator.iterLL(inVars,ExpressionIterator.circuitIter());
        ExpressionIterator.iterL(outVars,ExpressionIterator.circuitIter());
        return this;
    }

    public void toAssignmentStatements(StatementBuffer assignments) {
        // change references
        ExpressionIterator.iterLL(inVars,ExpressionIterator.changeRefIter());
        ExpressionIterator.iterL(outVars,ExpressionIterator.changeRefIter());

        toAssignmentStatements_NoChangeRef(assignments);
    }

    public void toAssignmentStatements_NoChangeRef(StatementBuffer assignments) {
        // make sure variables have references
        ExpressionIterator.iterLL(inVars,ExpressionIterator.addRefIter());
        ExpressionIterator.iterL(outVars,ExpressionIterator.addRefIter());

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
        ExpressionIterator.iterLL(
            inVars,ExpressionIterator.toCircuitIter(circuit),ExpressionIterator.toCBrkIter(this.circuit)
        );
        circuit.print("] ] OUTPUTS [ ");

        // output variables
        ExpressionIterator.iterL(outVars,ExpressionIterator.toCircuitIter(circuit));
        circuit.print("]");

        circuit.println("\t// " + this.toString());
    }

    public String toString() {
        return "exo_compute #" + Integer.toString(compNum) +
               " inVectors=" + inVars.size() +
               " outVars=" + outVars.size();
    }
}
