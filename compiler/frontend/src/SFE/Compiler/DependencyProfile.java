package SFE.Compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.List;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import ccomp.CBuiltinFunctions;

public class DependencyProfile {
	public DependencyProfile() {
	}

	/**
	 * Read in a .circuit.tac file, and produce a .profile.tac file which
	 * indicates which lines are used, and how often.
	 * 
	 * Output format:
	 * 
	 * a b c d e
	 * 
	 * a - line number b - 1 if used (output lines count as used), 0 otherwise. If
	 * b is 0, c, d, and e are ignored. c - is output line d - reference count e -
	 * 'kill point', line number of last referencer
	 * 
	 * Note that lines where b = 0 are not usually output.
	 * 
	 * 
	 * The lines are written out in reverse line number order. Use .tac to switch
	 * the lines in the .profile.tac file produced.
	 */
	public void makeProfile(File circuitBackwards, File outFile) {
		Set<String> outputVariables = new HashSet();

		Map<Integer, int[]> refDatas = new HashMap();

		// ReadBackwardsTextFile rbtf = new ReadBackwardsTextFile(circuit, 1 << 20);
		// //Use about 1 megabyte of memory

		try {
			// Construct the BufferedReader object
			BufferedReader bufferedReader = new BufferedReader(new FileReader(
			    circuitBackwards));

			PrintWriter out = new PrintWriter(outFile);
			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				// Process the data, here we just print it out
				parseAssignment(line, out, outputVariables, refDatas);
			}
			bufferedReader.close();
			out.close();
		} catch (Throwable e) {
			throw new RuntimeException(e);
		}

		/*
		 * 
		 * try { in2 = new Scanner(circuitBackwards); } catch (FileNotFoundException
		 * e1) { throw new RuntimeException(e1); } int lastLineNumber =
		 * Integer.MAX_VALUE; while(in2.hasNextLine()){
		 * 
		 * String line = in2.nextLine();
		 * 
		 * parseAssignment(lineNumber, output, in, out, refDatas); } }
		 */
	}

	private static Pattern wspace = Pattern.compile("\\s+");
	public static boolean printUpdates = false;
	private static int lastPrint = 0;

	private void parseAssignment(String line, PrintWriter out,
	    Set<String> outputVariables, Map<Integer, int[]> refDatas) {
		String[] in = wspace.split(line);
		int in_p = 0;
		String firstTerm = in[in_p++];

		int lineNumber = Integer.parseInt(firstTerm);
		if (printUpdates) {
			if (Math.abs(lineNumber - lastPrint) > 1000000) {
				lastPrint = lineNumber;
				System.out.println("Profiling has " + lineNumber + " lines to go");
			}
		}

		String type = in[in_p++];
		if (type.equals("input")) {
			out.printf("%d 1 0 -1 -1\n", lineNumber); // This line is used, it is not
			                                          // output, reference count and
			                                          // kill points don't matter for
			                                          // input lines
		} else if (type.equals("split")) {
			// Read output lines
			int[] ref = new int[2];
			boolean used = false;
			while (true) {
				String next = in[in_p++];
				if (next.equals("[")) {
					break;
				}
				// Integer tokens are output lines
				try {
					int i = Integer.parseInt(next);
					if (refDatas.containsKey(i)) {
						int[] subRef = refDatas.remove(i);
						ref[0] += subRef[0];
						ref[1] = Math.max(ref[1], subRef[1]);
						used = true;
					}
				} catch (Throwable e) {
				}
			}
			if (used) {
				while (true) {
					String next = in[in_p++];
					if (next.equals("]")) {
						break;
					}
					// Integer tokens are variable references
					try {
						int i = Integer.parseInt(next);

						int[] oldCount = refDatas.get(i);
						if (oldCount == null) {
							// Last reference to this variable found. Set i's kill point to
							// this line.
							oldCount = new int[] { 1, lineNumber };
						} else {
							oldCount[0]++;
						}
						refDatas.put(i, oldCount);
					} catch (Throwable e) {
					}
				}
				// Emit statement
				// Print out this line
				out.printf("%d 1 %d %d %d\n", lineNumber, 0, // split statement is never
				                                             // output.
				    ref[0], ref[1]); // This line is used, it may be output output ref
				                     // count and kill point
			}
		} else {
			boolean output = false;
			if (type.equals("output")) {
				// We only want the final assignment to output variables.
				String varName = wspace
				    .split(line.substring(line.indexOf("//") + 2), 2)[0];
				if (!outputVariables.contains(varName)) {
					outputVariables.add(varName);
					output = true;
					// Ensure that the variable is referenced.
					if (!refDatas.containsKey(lineNumber)) {
						refDatas.put(lineNumber, new int[] { 1, Integer.MAX_VALUE }); // Reference
						                                                              // from
						                                                              // infinity
					}
				}
			}

			// Don't remove RAMPUTs, HASHFREEs, or ASSERT_ZEROs, these never have
			// referencers but they must be preserved.
			if (type.equals(CBuiltinFunctions.RAMPUT_NAME)
			    || type.equals(CBuiltinFunctions.ASSERT_ZERO_NAME)
			    || type.equals(CBuiltinFunctions.HASHFREE_NAME)
			    || type.equals("printf")) {
				if (refDatas.containsKey(lineNumber)) {
					throw new RuntimeException("Assertion error " + lineNumber);
				}
				refDatas.put(lineNumber, new int[] { 1, Integer.MAX_VALUE }); // Reference
				                                                              // from
				                                                              // infinity
			}

			if (type.equals(CBuiltinFunctions.RAMPUT_ENHANCED_NAME)) {
				if (refDatas.containsKey(lineNumber)) {
					throw new RuntimeException("Assertion error " + lineNumber);
				}
				refDatas.put(lineNumber, new int[] { 1, Integer.MAX_VALUE }); // Reference
				                                                              // from
				                                                              // infinity
			}

            // preserve exo_computes!
            if (type.equals(CBuiltinFunctions.EXO_COMPUTE_NAME)) {
                if (refDatas.containsKey(lineNumber)) {
                    throw new RuntimeException("Assertion error (exo) " + lineNumber);
                }
                refDatas.put(lineNumber, new int[] { 1, Integer.MAX_VALUE });   // ref from infty

                // parse this whole line so that we can add refs to the inputs and outputs
                final CompiledStatement.ParsedExoCompute p = CompiledStatement.exoComputeParser(new CompiledStatement.ArrayIterator<String>(in,in_p));

                // the inputs are referenced by this line, so add a reference as appropriate
                for (List<String> thisL : p.inVarsStr) {
                for (String s : thisL) {
                    final int i = Integer.parseInt(s);

                    int[] oldCount = refDatas.get(i);
                    if (oldCount == null) {
                        oldCount = new int[] { 1, lineNumber };
                    } else {
                        oldCount[0]++;
                    }
                    refDatas.put(i, oldCount);
                }}

                // the outputs should be referenced from infinity so they're never killed
                // is this a godawful hack? But probably we have to make sure that there is
                // always a place for the prover to insert its nondeterminism, right?
                for (String s : p.outVarsStr) {
                    final int i = Integer.parseInt(s);
                    refDatas.put(i, new int[] { 1, Integer.MAX_VALUE });
                }
            }

			int[] refs = null;
			if (type.equals(CBuiltinFunctions.RAMGET_NAME)
			    || type.equals(CBuiltinFunctions.HASHGET_NAME)
			    || type.equals(GenericGetStatement.GENERIC_GET_STATEMENT_STR)) {
				refs = getRefDataForAll(in, refDatas, "Y", "]");
			} else if (type.equals(CBuiltinFunctions.HASHPUT_NAME)) {
				refs = getRefDataForAll(in, refDatas, "HASH_OUT", "NUM_X");
			} else if (type.equals(CBuiltinFunctions.RAMGET_ENHANCED_NAME)) {
				refs = getRefDataForAll(in, refDatas, "VALUE", "inputs");
			} else {
				if (refDatas.containsKey(lineNumber)) {
					refs = refDatas.remove(lineNumber);
				}
			}

			// This is an assignment statement. Is it used?
			if (refs != null) {
				// Print out this line
				out.printf("%d 1 %d %d %d\n", lineNumber, output ? 1 : 0, refs[0],
				    refs[1]); // This line is used, it may be output output ref count
				              // and kill point

				// Mark all dependencies of the RHS of this assignment statement as used
				while (true) {
					String next = in[in_p++];
					if (next.equals("[")) {
						break;
					}
				}
				while (true) {
					String next = in[in_p++];
					if (next.equals("]")) {
						break;
					}
					// Integer tokens are variable references
					try {
						int i = Integer.parseInt(next);

						int[] oldCount = refDatas.get(i);
						if (oldCount == null) {
							// Last reference to this variable found. Set i's kill point to
							// this line.
							oldCount = new int[] { 1, lineNumber };
						} else {
							oldCount[0]++;
						}
						refDatas.put(i, oldCount);
					} catch (Throwable e) {
					}
				}
			} else {
				// Don't output unused lines
			}
		}
	}

	/**
	 * Read until start_after is found, and then check reference data for all
	 * variables listed up until end (noninclusive) is found.
	 * 
	 * Note: All tokens between start_after and end must be variables.
	 */
	private int[] getRefDataForAll(String[] in, Map<Integer, int[]> refDatas,
	    String start_after, String end) {
		// Used if any of the store bits are used
		int[] refs = null;
		int in_p = 0;
		while (true) {
			String next = in[in_p++];
			if (next.equals(start_after)) {
				break;
			}
		}
		while (true) {
			String next = in[in_p++];
			if (next.equals(end)) {
				break;
			}
			int outBit = Integer.parseInt(next);
			if (refDatas.containsKey(outBit)) {
				int[] subRef = refDatas.remove(outBit);
				if (refs == null) {
					refs = new int[2];
				}
				refs[0] += subRef[0];
				refs[1] = Math.max(refs[1], subRef[1]); // Keep the whole set until the
				                                        // last one is no long
				                                        // referenced.
			}
		}
		return refs;
	}
}
