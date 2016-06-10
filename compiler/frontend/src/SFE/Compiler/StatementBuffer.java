package SFE.Compiler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;

/**
 * A modifiable buffer to hold a partial representation of the computation being
 * compiled as assignment statements. As the buffer exceeds its capacity, or
 * when computation completes, assignment statements in the buffer are pushed to
 * disk. Thus, if a statement is known to be dead code shortly after it is
 * created, it can be removed from the buffer before being sent to the disk.
 */
public class StatementBuffer {
	/*
	 * Instance variables
	 */
	private TreeMap<Integer, StatementWithOutputLine> buffer;
	private Map<String, List<AssignmentStatement>> temporaryVarAssignments;
	private Set<Integer> liveVariables; // Holds assignment line numbers which
										// are
										// in the buffer and could still be
										// referenced in the future
	private Set<Integer> cleanupQueue; // Holds variables which are in the
										// buffer
										// but could not be referenced in the
										// future
	private int maxSize;
	private PrintWriter out;
	private int mostRecentStatement = -1; // The output line number of the
											// latest
											// statement in the program which
											// has
											// been analyzed
	private Stack<Boolean> uncertainty; // Used to ban statements which affect
										// external state (putdb) from being
										// emitted inside if statements

	private Stack<LvalExpression> conditions;

	private Stack<Boolean> branches;

	/**
	 * Creates an intermediate statement buffer which writes statements out to a
	 * print writer.
	 */
	public StatementBuffer(int maxSize, PrintWriter out) {
		buffer = new TreeMap();
		liveVariables = new HashSet();
		cleanupQueue = new HashSet();
		temporaryVarAssignments = new HashMap();
		uncertainty = new Stack();
		conditions = new Stack<LvalExpression>();
		branches = new Stack<Boolean>();
		this.maxSize = maxSize;
		this.out = out;
	}

	public boolean add(StatementWithOutputLine s) {
		if ((s instanceof RamPutStatement) && !uncertainty.isEmpty()) {
			throw new RuntimeException(
					"I don't know how to expand putdb when it is used inside of an if statement.");
		}

		if (Optimizer.isFirstPass()) {
			if (s instanceof AssignmentStatement) {
				AssignmentStatement as = (AssignmentStatement) s;

				// Temporary variables (which are denoted by : and then a suffix
				// on top
				// of the base variable)
				// are _never_ referenced if the base variable is not
				// referenced.

				// This is different than bit variables (which are separated by
				// BitString.BIT_SEPARATOR_CHAR) -
				// using the LvalExpression of the base variable, you can get
				// the bits
				// without generating references
				// for the lval. In other words, the bits can in many cases be
				// used
				// without using the base - so
				// they don't act like temporaries.

				// Is this a temporary variable assignment? We can check from
				// the name:
				String lvName = as.getLHS().getName();
				int lastColon = lvName
						.lastIndexOf(Expression.TEMPORARY_SEPARATOR); // Colons
																		// are
																		// not
																		// allowed
																		// in
																		// variable
																		// names.
				if (lastColon != -1) {
					String parentVar = lvName.substring(0, lastColon);
					if (parentVar.endsWith("[$]")) {
						// Currently, arrayExpression / arrayStatement don't
						// play nicely
						// with this optimization.
						// System.out.println(lvName);
					} else {
						List<AssignmentStatement> tmps = temporaryVarAssignments
								.get(parentVar);
						if (tmps == null) {
							tmps = new ArrayList(2);
						}
						tmps.add(as);
						temporaryVarAssignments.put(parentVar, tmps);
					}
				}
			}
		}

		int outputLine = s.getOutputLine();

		/*
		 * if (outputLine >= 175){ System.out.println(outputLine); }
		 */

		buffer.put(outputLine, s);
		liveVariables.add(outputLine); // Mark the variable as live.
		mostRecentStatement = outputLine;

		while (buffer.size() > maxSize) {
			emitStatement();
		}

		return true;
	}

	public void popUncertainty() {
		uncertainty.pop();
	}

	public void pushUncertainty() {
		uncertainty.push(true);
	}

	public void pushCondition(LvalExpression condition, boolean thenOrElse) {
		conditions.push(condition);
		branches.push(thenOrElse);
	}

	public LvalExpression getCondition() {
		if (conditions.empty()) {
			return null;
		}
		return conditions.peek();
	}

	public boolean getBranch() {
		if (branches.empty()) {
			return false;
		}
		return branches.peek();
	}

	public LvalExpression popCondition() {
		branches.pop();
		return conditions.pop();
	}

	/**
	 * Get (and remove) all temporary variable assignments for varName.
	 */
	public List<AssignmentStatement> getTemporaryVariableAssignmentsFor(
			String varName) {
		List<AssignmentStatement> toRet = new ArrayList(2);
		List<AssignmentStatement> tempVars = temporaryVarAssignments
				.remove(varName);
		if (tempVars != null) {
			for (AssignmentStatement as : tempVars) {
				// Is this still a live assignment statement?
				if (liveVariables.contains(as.getOutputLine())) {
					toRet.add(as);
				}
			}
		}

		return toRet;
	}

	/**
	 * Attempts to remove the given statement from the buffer. If the statement
	 * has already been written to the output writer, nothing occurs.
	 */
	public boolean remove(int outputLine) {
		if (buffer.isEmpty()) {
			return false;
		}
		liveVariables.remove(outputLine);
		cleanupQueue.remove(outputLine);
		return buffer.remove(outputLine) != null;
	}

	public void flush() {
		mostRecentStatement = Integer.MAX_VALUE; // All statements are now past
													// their kill points.
		while (buffer.size() > 0) {
			emitStatement();
		}
	}

	private void emitStatement() {

		StatementWithOutputLine remove = buffer.firstEntry().getValue();

		int outputLine = remove.getOutputLine();

		if (remove instanceof AssignmentStatement
				&& ((AssignmentStatement) remove).getLHS().getKillPoint() < mostRecentStatement) {
			// If the assignment has not been referenced yet,
			// and the variable is past its liveness point, (note - output
			// variables
			// remain alive indefinitely)
			// don't output it!
			AssignmentStatement as = (AssignmentStatement) remove;
			if (!as.getLHS().isReferenced() && !as.getLHS().isOutput()) {
				callbackAssignment(as);
				cleanupAssignmentData(as);
				return;
			}
		}
		// TODO do something for RAMGET_FAST, it is also a kind of assignment.

		remove = buffer.remove(remove.getOutputLine());

		// toCircuit is called from here.
		((OutputWriter) remove).toCircuit(null, out);
		
		// Allow the garbage collector to clean up lvals referenced by the
		// output
		// line
		if (remove instanceof AssignmentStatement) {
			// If the assignment is no longer alive, make sure its assignment
			// data has
			// been freed
			AssignmentStatement as = (AssignmentStatement) remove;
			if (cleanupQueue.contains(outputLine)) {
				cleanupQueue.remove(outputLine);
				cleanupAssignmentData(as);
			}
		}
		liveVariables.remove(outputLine);
		cleanupQueue.remove(outputLine);

		if (cleanupQueue.size() + liveVariables.size() != buffer.size()) {
			throw new RuntimeException();
		}

		// if (temporaryVarAssignments.size() > 10){
		// throw new RuntimeException();
		// }

		// If we're trying to minimize memory, we may drop an assignment
		// statement
		// from memory
		// after it's been written, if its memory footprint is high.
		if (remove instanceof AssignmentStatement) {
			AssignmentStatement as = (AssignmentStatement) remove;
			cleanupAssignmentData(as);
		}
	}

	/*
	 * Mark the assignment as not possible to be referenced in the future.
	 */
	public void cleanupAssignmentData(AssignmentStatement remove) {

		int line = remove.getOutputLine();
		// If remove is live, switch it to the cleanupQueue
		if (liveVariables.contains(line)) {
			// Mark the variable as dead and take care of it later.
			liveVariables.remove(line);
			cleanupQueue.add(line);
			return;
		}
		// Otherwise, if remove is in cleanup queue, return;
		if (cleanupQueue.contains(line)) {
			return;
		}
		// If remove is not in cleanupQueue or liveVariables, then clean it up
		// immediately
		// Give the LHS a new assignment statement that has no RHS.
		/*
		 * AssignmentStatement filler = (AssignmentStatement)
		 * remove.duplicate(); filler.clearRHS();
		 * filler.getLHS().setAssigningStatement(filler, true);
		 */

		remove.clearMemoryIntensiveRHS();
	}

	/**
	 * Try to call back the assignment from the buffer, so it doesn't get
	 * written to disk
	 */
	public void callbackAssignment(AssignmentStatement oldAssignment) {

		int outputLine = oldAssignment.getOutputLine();

		if (outputLine == 6823) {
			// System.out.println(outputLine);
		}

		/*
		 * We don't accurately keep track of real reference counts; there are
		 * cases in which an LHS thinks it has references when it really does
		 * not: So, leave the caller of this method to be careful.
		 * 
		 * if (oldAssignment.getLHS().isReferenced()) { throw new
		 * RuntimeException(
		 * "Can not call back an assignment to an LHS which has references."); }
		 */

		// Try to remove oldAssignment before it gets to the disk:
		boolean successfullyErased = this.remove(outputLine);
		if (successfullyErased) {
			oldAssignment.clearRHS();

			// Remove the references this old assignment was imposing:
			((AssignmentStatement) oldAssignment).removeAllReferences();
			// Prevent future references to the removed assignment, by removing
			// any
			// references to its LHS from the memo
			/*
			 * Expression oldExpr =
			 * ((AssignmentStatement)oldAssignment).getRHS(); if (oldExpr !=
			 * null){ LvalExpression memo = Optimizer.getLvalFor(oldExpr); if
			 * (memo != null){ if
			 * (memo.toString().equals(oldAssignment.getLHS().toString())){
			 * Optimizer.removeLvalFor(oldExpr); } } }
			 */

		} else {
		}
	}
}
