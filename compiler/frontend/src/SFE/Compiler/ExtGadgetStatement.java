package SFE.Compiler;

import java.io.PrintWriter;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

import SFE.Compiler.util.ExpressionIterator;
import ccomp.CBuiltinFunctions;

public class ExtGadgetStatement extends StatementWithOutputLine implements OutputWriter {
    // each list in inVars corresponds to one of the structs in inLs
    private final List<LvalExpression> inVars;
    // each list in outVars corresponds to one of the structs in outStrP;
    private final List<LvalExpression> outVars;
    
    private final int compNum;
    private final int numIntermediateVar;
    private final long intermediateVarOffset;
    private int outputLine = -1;

    // circuit writer for use by some ExpressionIterator.IterLLCalls
    private PrintWriter circuit;

    private static long GLOBAL_INTERMEDIATE_VAR_COUNT = 0;

    public ExtGadgetStatement(List<LvalExpression> inVars, List<LvalExpression> outVars, int compNum) {
        this.inVars = inVars;
        this.outVars = outVars;
        this.compNum = compNum;
        this.numIntermediateVar = queryIntermediateVars();
        this.intermediateVarOffset = GLOBAL_INTERMEDIATE_VAR_COUNT;
        GLOBAL_INTERMEDIATE_VAR_COUNT += this.numIntermediateVar;
    }

    public Statement duplicate() {
        return new ExtGadgetStatement(inVars, outVars, compNum);
    }

    public Statement toSLPTCircuit(Object obj) {
         // resolve input and output variables to their corresponding field element
         ExpressionIterator.iterL(inVars,ExpressionIterator.circuitIter());
         ExpressionIterator.iterL(outVars,ExpressionIterator.circuitIter());
         return this;
    }
   
    public void toAssignmentStatements(StatementBuffer assignments) {
        // change references
        ExpressionIterator.iterL(inVars,ExpressionIterator.changeRefIter());
        ExpressionIterator.iterL(outVars,ExpressionIterator.changeRefIter());

        toAssignmentStatements_NoChangeRef(assignments);
    }

    public void toAssignmentStatements_NoChangeRef(StatementBuffer assignments) {
        // make sure variables have references
        ExpressionIterator.iterL(inVars,ExpressionIterator.addRefIter());
        ExpressionIterator.iterL(outVars,ExpressionIterator.addRefIter());

        outputLine = Program.getLineNumber();
        assignments.add(this);
    }

    public int getOutputLine() {
        return outputLine;  // returns -1 if not yet assigned
    }

    public void toCircuit(Object obj, PrintWriter circuit) {
        this.circuit = circuit;

        // ext_gadget
        circuit.print(getOutputLine() + " " + CBuiltinFunctions.EXT_GADGET_NAME +
                      " GADGETID " + Integer.toString(compNum) + " INPUTS [ ");

        // input variables
        ExpressionIterator.iterL(inVars,ExpressionIterator.toCircuitIter(circuit));
        circuit.print("] OUTPUTS [ ");

        // output variables
        ExpressionIterator.iterL(outVars,ExpressionIterator.toCircuitIter(circuit));

        // intermediate variables
        circuit.print("] INTERMEDIATE ");
        circuit.print(numIntermediateVar);
        circuit.print(" OFFSET ");
        circuit.print(intermediateVarOffset);

        circuit.println("\t// " + this.toString());
    }

    public String toString() {
        return "ext_gadget #" + Integer.toString(compNum) +
               " inVectors=" + inVars.size() +
               " outVars=" + outVars.size() +
               " intermediateVars=" + numIntermediateVar;
    }

    private int queryIntermediateVars() {
        try {
            Process p = Runtime.getRuntime().exec("../pepper/bin/gadget" + compNum + " size");
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            return Integer.parseInt(reader.readLine());   
        } catch (IOException e) {
            throw new RuntimeException(e + "\nDoes executable ./bin/gadget" + compNum + " exist?\n");
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while calling into gadget.");
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid format. Does ./bin/gadget" + compNum + " return the number of intermediate variables of the gadget?");
        }
    }
}
