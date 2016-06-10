package SFE.Compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.TreeSet;

import ccomp.CBuiltinFunctions;

/**
 * Converts .circuit files to .spec files (constraints)
 * 
 * The conversion is different depending on whether we are producing Zaatar or
 * Ginger constraints.
 */
public class ConstraintWriter {
	private File circuitFile;

	public ConstraintWriter(String circuitFile) throws IOException {
		this.circuitFile = new File(circuitFile);
		if (!this.circuitFile.exists()) {
			throw new FileNotFoundException();
		}
	}

	private PrintWriter out;
	private Map<Integer, String> inputVariables;
	private Set<Integer> outputVariables;

	public void toConstraints(String constraintsFile)
			throws NumberFormatException, IOException {
		out = new PrintWriter(constraintsFile);

		inputVariables = new TreeMap<Integer, String>();
		outputVariables = new TreeSet<Integer>();
		int uniquifier = 0;

        // exoCompute output variables; we use this to hold comments and to check that we've found them all
        final HashMap<String, Boolean> exoCompOutputs = new HashMap<String, Boolean>();

        // first pass - look for exo_compute statements and gather up
        // their output variables so that we can convert them to normal
        // variables
        // think: nondeterministic inputs aren't inputs from the circuit POV
        // but they are behaviorally. So until now we have treated them as
        // inputs, but now we convert them to normal variables
        {
            final BufferedReader br = new BufferedReader(new FileReader(circuitFile));

            String line = null;
            while ((line = br.readLine()) != null) {
                // greater than zero because we know there's a line number and a space first!
                if (line.indexOf(CBuiltinFunctions.EXO_COMPUTE_NAME) > 0) {
                    final Scanner in = new Scanner(line);
                    final String lineNo = in.next();
                    in.next();  // CBuiltinFunctions.EXO_COMPUTE_NAME
                    final CompiledStatement.ParsedExoCompute p = CompiledStatement.exoComputeParser(in);

                    for (String s : p.outVarsStr) {
                        if (null != exoCompOutputs.get(s)) {
                            throw new RuntimeException("Error: two EXO_COMPUTE statements write the same variable.");
                        }
                        exoCompOutputs.put(s,true);   // for now, just put in a dummy string, we'll fill this in in the next pass
                    }
                }
            }
        }

        // second pass, input variables
        // skip exo_compute outputs here; they're not actually circuit inputs
		{
			BufferedReader bufferedreader = new BufferedReader(new FileReader(
					circuitFile));
			out.println("START_INPUT");
			String line = null;
			while ((line = bufferedreader.readLine()) != null) {
				Scanner in = new Scanner(line);
				String firstTerm = in.next();
				if (firstTerm.equals("shortcircuit") || firstTerm.equals("}")) {
					continue;
				}
				int varNum = new Integer(firstTerm);
				if (in.next().equals("input")) {
                    if (null == exoCompOutputs.get(firstTerm)) {
                        // normal input variable, since it's not in the exoCompOutputs
                        String comment = line.split("//")[1];
                        inputVariables.put(varNum, comment);
                        String variableName = "I" + varNum;
                        out.println(variableName + " //" + comment);
                    }
				}
			}
			out.println("END_INPUT");
			bufferedreader.close();
		}

		out.println();

        // third pass - output variables
		{
			BufferedReader bufferedreader = new BufferedReader(new FileReader(
					circuitFile));
			out.println("START_OUTPUT");
			TreeMap<Integer, String> outputVarLines = new TreeMap();
			String line = null;
			while ((line = bufferedreader.readLine()) != null) {
				Scanner in = new Scanner(line);
				String firstTerm = in.next();
				if (firstTerm.equals("shortcircuit") || firstTerm.equals("}")) {
					continue;
				}
				int varNum = new Integer(firstTerm);
				if (in.next().equals("output")) {
					outputVariables.add(varNum);
					String variableName = "O" + varNum;
					outputVarLines.put(varNum,
							variableName + " //" + line.split("//")[1]);
				} else {
				}
			}
			// Outputs sorted.
			for (String line_ : outputVarLines.values()) {
				out.println(line_);
			}
			out.println("END_OUTPUT");
			bufferedreader.close();
		}

		out.println();

        // fourth pass - internal variables
		{
			BufferedReader bufferedreader = new BufferedReader(new FileReader(
					circuitFile));
			out.println("START_VARIABLES");

			if (Optimizer.optimizationTarget == Optimizer.Target.GINGER) {
				// In GINGER, we need an additional variable for input binding
				for (Entry<Integer, String> i : inputVariables.entrySet()) {
					String variableName = "V" + i.getKey();
					out.println(variableName + " //" + i.getValue());
				}
			}
			String line = null;

			TreeMap<String, Integer> operation_counts = new TreeMap<String, Integer>();
			operation_counts.put(CBuiltinFunctions.RAMGET_NAME, 0);
			operation_counts.put(CBuiltinFunctions.RAMPUT_NAME, 0);
			operation_counts.put(CBuiltinFunctions.HASHGET_NAME, 0);
			operation_counts.put(CBuiltinFunctions.HASHPUT_NAME, 0);
			operation_counts.put(CBuiltinFunctions.RAMGET_ENHANCED_NAME, 0);
			operation_counts.put(CBuiltinFunctions.RAMPUT_ENHANCED_NAME, 0);
            operation_counts.put(CBuiltinFunctions.EXO_COMPUTE_NAME, 0);

			while ((line = bufferedreader.readLine()) != null) {
				Scanner in = new Scanner(line);
				String firstTerm = in.next();
				if (firstTerm.equals("shortcircuit") || firstTerm.equals("}")) {
					continue;
				}
				int varNum = new Integer(firstTerm);
				String type = in.next();
				if (type.equals("split")) {
					// Add all variables
					String varName = (line.split("//")[1].split("\\s+")[0]);
					// Remove BIT_SEPARATOR_CHAR + "0"
					varName = allButLast(varName, 2);

					toConstraints_addBitVariablesList(in, varName);
				} else if (type
						.equals(GenericGetStatement.GENERIC_GET_STATEMENT_STR)) {
					// Add the output variables to the list
					String lookupType = in.next();
					while (true) {
						String got = in.next();
						if (got.equals("Y")) {
							break;
						}
					}
					String varName = "genericget_" + lookupType + "_"
							+ (uniquifier++);
					toConstraints_addBitVariablesList(in, varName);
				} else if (type.equals(CBuiltinFunctions.RAMGET_NAME)
                        || type.equals(CBuiltinFunctions.EXO_COMPUTE_NAME)
						|| type.equals(CBuiltinFunctions.RAMPUT_NAME)
						|| type.equals(CBuiltinFunctions.HASHGET_NAME)
						|| type.equals(CBuiltinFunctions.HASHPUT_NAME)
						|| type.equals(CBuiltinFunctions.RAMGET_ENHANCED_NAME)
						|| type.equals(CBuiltinFunctions.RAMPUT_ENHANCED_NAME)) {
					increment(operation_counts, type);
					if (type.equals(CBuiltinFunctions.RAMGET_ENHANCED_NAME)) {
						in.next(); // "VALUE"
						String variableName = in.next();
						out.println("V" + variableName + " //"
								+ line.split("//")[1]);
					}
					if (type.equals(CBuiltinFunctions.RAMPUT_ENHANCED_NAME)) {
						String variableName = in.next();
						out.println("V" + variableName + " //"
								+ line.split("//")[1]);
					}
					// Do nothing else.
				} else if (type.equals(CBuiltinFunctions.ASSERT_ZERO_NAME)
						|| type.equals("printf")
						|| type.equals(CBuiltinFunctions.HASHFREE_NAME)) {
					// Do nothing.
				} else {
					if (!inputVariables.containsKey(varNum) && !outputVariables.contains(varNum)) {
                        // this might fail silently, but that's OK, we check later that we got them all
                        exoCompOutputs.remove(firstTerm);

						String variableName = "V" + varNum;
						out.println(variableName + " //" + line.split("//")[1]);
					} else {
                        // input and output variables already taken care of
					}
				}
			}
			out.println("END_VARIABLES");
			bufferedreader.close();

            assert exoCompOutputs.size() == 0 :
                "exoCompOutputs has remaining elements, indicating that we failed to add all its contents to the VARIABLES list.";

			// Print to stdout the op counts:
			for (Entry<String, Integer> entry : operation_counts.entrySet()) {
				System.out
						.println("metric_num_occurrences_"
								+ entry.getKey().toLowerCase() + " "
								+ entry.getValue());
			}
		}

		out.println();
		{
			out.println("START_CONSTRAINTS");
			if (Optimizer.optimizationTarget == Optimizer.Target.GINGER) {
				// In GINGER, we need an additional variable for input binding
				for (Integer i : inputVariables.keySet()) {
					out.println("I" + i + " - V" + i); // Vi is the unknown
				}
			}
			BufferedReader bufferedReader = new BufferedReader(new FileReader(
					circuitFile));

			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				String[] lines = line.split("\\s+");
				if (lines[0].equals("}")) {
					out.println("}");
				} else if (lines[0].equals("shortcircuit")) {
					lines[3] = getConstraintVarName(new Integer(lines[3])); // Add
																			// variable
																			// prefix
																			// to
																			// the
																			// variable
					lines[5] = lines[5].substring(1); // remove C prefix from
					// the constant
					for (String k : lines) {
						out.print(k + " ");
					}
					out.println();
				} else {
                    compileConstraintsLine(line);
				}
			}
			out.println("END_CONSTRAINTS");
			bufferedReader.close();
		}

		out.println();

		out.close();
	}

	private static void increment(TreeMap<String, Integer> operation_counts,
			String type) {
		Integer old = operation_counts.get(type);
		if (old == null) {
			old = 0;
		}
		old++;
		operation_counts.put(type, old);
	}

	/**
	 * Parse tokens as integers (until you can't), outputting a line in the
	 * VARIABLES block for each. Use varName as a prefix for naming the
	 * variables.
	 * 
	 * Each variable output has type uint bits 1.
	 */
	private void toConstraints_addBitVariablesList(Scanner in, String varName) {
		ArrayList<Integer> vars = new ArrayList();
		while (true) {
			String got = in.next();
			try {
				int val = Integer.parseInt(got);
				vars.add(val);
			} catch (Throwable e) {
				break;
			}
		}
		for (int i = 0; i < vars.size(); i++) {
			out.println("V" + vars.get(i) + " //" + varName
					+ BitString.BIT_SEPARATOR_CHAR + i + " uint bits 1");
		}
	}

	private String allButLast(String a, int i) {
		return a.substring(0, a.length() - i);
	}

	private int nextInt(Scanner in) {
		return Integer.parseInt(in.next());
	}

	private void compileConstraintsLine(String line) {
		Scanner in = new Scanner(line);

		in = new Scanner(line);
        String varNumStr = in.next();
		int varNum = Integer.parseInt(varNumStr);
		String variableName = getConstraintVarName(varNum);
		String type = in.next();
		if (type.equals("output")) {
			type = in.next();
		}
		// Sequential
		if (type.equals("gate")) {
			String gateType = in.next();
			in.next();
			in.next();
			// if (line.contains("!=") || line.contains("<")) {
			if (gateType.equals("!=") || gateType.equals("<")
					|| gateType.equals("%") || gateType.equals("/")) {
				compileNonPolyConstraint(variableName, in);
				// } else if (gateType.equals("getdb")){
				// compileGetDbConstraint(variableName, in);
			} else {
                // first check if this poly constraint is one of the spurious
                // assignments we inserted into the circuit in order to make a
                // placeholder variable for the exo_compute output
                compilePolyConstraint(variableName, in);
			}
		} else if (type.equals("split")) {
			compileSplitConstraint(in);
        } else if (type.equals(CBuiltinFunctions.EXO_COMPUTE_NAME)) {
            compileExoComputeConstraint(in);
		} else if (type.equals(CBuiltinFunctions.RAMGET_ENHANCED_NAME)) {
			compileRAMGetEnhancedConstraint(in);
		} else if (type.equals(CBuiltinFunctions.RAMPUT_ENHANCED_NAME)) {
			compileRAMPutEnhancedConstraint(in);
		} else if (type.equals(CBuiltinFunctions.RAMGET_NAME)) {
			compileGetDbConstraint(in);
		} else if (type.equals(CBuiltinFunctions.RAMPUT_NAME)) {
			compilePutDbConstraint(in);
		} else if (type.equals(CBuiltinFunctions.HASHGET_NAME)) {
			compileHashGetConstraint(in);
		} else if (type.equals(CBuiltinFunctions.HASHPUT_NAME)) {
			compileHashPutConstraint(in);
		} else if (type.equals(GenericGetStatement.GENERIC_GET_STATEMENT_STR)) {
			compileGenericGetConstraint(in);
		} else if (type.equals(CBuiltinFunctions.HASHFREE_NAME)) {
			compileHashFreeConstraint(in);
		} else if (type.equals(CBuiltinFunctions.ASSERT_ZERO_NAME)) {
			compileAssertZeroConstraint(in);
		} else if (type.equals("printf")) {
			compilePrintfConstraint(in);
		} else if (type.equals("input")) {
			// Nothing to do.
		} else {
			throw new RuntimeException(
					"I don't know how to convert circuit line to constraints: "
							+ line);
		}
	}

	private void compileAssertZeroConstraint(Scanner in) {
		in.next(); // "inputs"
		in.next(); // "["

		String varName = getConstraintVarName(in.nextInt());

		out.println("ASSERT_ZERO " + varName);
	}

	private void compilePrintfConstraint(Scanner in) {
		StringBuffer formatString = new StringBuffer();
		while (true) {
			String token = in.next();
			if (token.equals("inputs")) {
				break;
			}
			formatString.append(token + " ");
		}
		in.next(); // "["

		StringBuffer args = new StringBuffer();
		int num_args = 0;
		while (true) {
			String token = in.next();
			if (token.equals("]")) {
				break;
			}
			args.append(getConstraintVarName(new Integer(token)) + " ");
			num_args++;
		}
		in.nextLine();

		String formatStr = formatString.toString();
		formatStr = formatStr.substring(0, formatStr.length() - 1); // has
																	// quotes

		out.println("PRINTF " + formatStr + " NUM_X " + num_args + " X " + args);
	}

	private void compileGetDbConstraint(String variableName, Scanner in) {
		in.next(); // ADDR
		StringBuffer addrs = new StringBuffer();
		while (true) {
			String got = in.next();
			if (got.equals("]")) {
				break;
			}
			addrs.append(getConstraintVarName(new Integer(got)));
		}
		in.nextLine();

		out.println("GETDB ADDR " + addrs + " Y " + variableName);
	}

	private int parseIntConst(Scanner in) {
		String next = in.next();
		if (next.charAt(0) != 'C') {
			throw new RuntimeException("Non constant argument: " + next);
		}
		return Integer.parseInt(next.substring(1));
	}

	private void compileHashFreeConstraint(Scanner in) {
		in.next(); // "inputs"
		in.next(); // "["
		in.next(); // "NUM_HASH_BITS"
		int num_hash_bits = parseIntConst(in);
		in.next(); // "HASH_IN"
		StringBuffer hashVars = new StringBuffer();
		for (int i = 0; i < num_hash_bits; i++) {
			hashVars.append(getConstraintVarName(in.nextInt()));
			hashVars.append(" ");
		}
		in.nextLine();

		out.println(CBuiltinFunctions.HASHFREE_NAME.toUpperCase()
				+ " NUM_HASH_BITS " + num_hash_bits + " HASH_IN " + hashVars);
	}

	private void compileHashPutConstraint(Scanner in) {
		in.next(); // "inputs"
		in.next(); // "["
		in.next(); // "NUM_HASH_BITS"
		int num_hash_bits = parseIntConst(in);
		in.next(); // "HASH_OUT"
		StringBuffer hashVars = new StringBuffer();
		for (int i = 0; i < num_hash_bits; i++) {
			hashVars.append(getConstraintVarName(in.nextInt()));
			hashVars.append(" ");
		}
		in.next(); // NUM_X
		int num_bits = parseIntConst(in);
		in.next(); // "X"
		StringBuffer value = new StringBuffer();
		for (int i = 0; i < num_bits; i++) {
			value.append(getConstraintTerm(in.next()));
			value.append(" ");
		}
		in.nextLine();

		out.println(CBuiltinFunctions.HASHPUT_NAME.toUpperCase()
				+ " NUM_HASH_BITS " + num_hash_bits + " HASH_OUT " + hashVars
				+ " NUM_X " + num_bits + " X " + value);
	}

	private void compileHashGetConstraint(Scanner in) {
		in.next(); // "inputs"
		in.next(); // "["
		in.next(); // "NUM_HASH_BITS"
		int num_hash_bits = parseIntConst(in);
		in.next(); // "HASH_IN"
		StringBuffer hashVars = new StringBuffer();
		for (int i = 0; i < num_hash_bits; i++) {
			hashVars.append(getConstraintTerm(in.next()));
			hashVars.append(" ");
		}
		in.next(); // NUM_Y
		int num_bits = parseIntConst(in);
		in.next(); // "Y"
		StringBuffer value = new StringBuffer();
		for (int i = 0; i < num_bits; i++) {
			value.append(getConstraintTerm(in.next()));
			value.append(" ");
		}
		in.nextLine();

		out.println(CBuiltinFunctions.HASHGET_NAME.toUpperCase()
				+ " NUM_HASH_BITS " + num_hash_bits + " HASH_IN " + hashVars
				+ " NUM_Y " + num_bits + " Y " + value);
	}

	private void compileGenericGetConstraint(Scanner in) {
		String lookup_type = in.next();
		in.next(); // "inputs"
		in.next(); // "["
		in.next(); // "NUM_HASH_BITS"
		int num_hash_bits = parseIntConst(in);
		in.next(); // "HASH_IN"
		StringBuffer hashVars = new StringBuffer();
		for (int i = 0; i < num_hash_bits; i++) {
			hashVars.append(getConstraintTerm(in.next()));
			hashVars.append(" ");
		}
		in.next(); // NUM_Y
		int num_bits = parseIntConst(in);
		in.next(); // "Y"
		StringBuffer value = new StringBuffer();
		for (int i = 0; i < num_bits; i++) {
			value.append(getConstraintTerm(in.next()));
			value.append(" ");
		}
		in.nextLine();

		out.println(GenericGetStatement.GENERIC_GET_STATEMENT_STR.toUpperCase()
				+ " " + lookup_type.toUpperCase() + " NUM_HASH_BITS "
				+ num_hash_bits + " HASH_IN " + hashVars + " NUM_Y " + num_bits
				+ " Y " + value);
	}

	private void compilePutDbConstraint(Scanner in) {
		in.next(); // "inputs"
		in.next(); // "["
		in.next(); // "ADDR"
		StringBuffer addrs = new StringBuffer();
		while (true) {
			String got = in.next();
			if (got.equals("NUM_X")) {
				break;
			}
			addrs.append(getConstraintVarName(new Integer(got)));
		}
		int num_bits = parseIntConst(in);
		in.next(); // "X"
		StringBuffer value = new StringBuffer();
		for (int i = 0; i < num_bits; i++) {
			value.append(getConstraintTerm(in.next()));
			value.append(" ");
		}
		in.nextLine();

		out.println(CBuiltinFunctions.RAMPUT_NAME.toUpperCase() + " ADDR "
				+ addrs + " NUM_X " + num_bits + " X " + value);
	}

	private void compileGetDbConstraint(Scanner in) {
		in.next(); // "inputs"
		in.next(); // "["
		in.next(); // "ADDR"
		StringBuffer addrs = new StringBuffer();
		while (true) {
			String got = in.next();
			if (got.equals("NUM_Y")) {
				break;
			}
			addrs.append(getConstraintVarName(new Integer(got)));
			addrs.append(" ");
		}
		int num_bits = parseIntConst(in);
		in.next(); // "Y"
		StringBuffer value = new StringBuffer();
		for (int i = 0; i < num_bits; i++) {
			value.append(getConstraintTerm(in.next()));
			value.append(" ");
		}
		in.nextLine();

		out.println(CBuiltinFunctions.RAMGET_NAME.toUpperCase() + " ADDR "
				+ addrs + " NUM_Y " + num_bits + " Y " + value);
	}

    private void compileExoComputeConstraint(Scanner in) {
        // parse the line
        final CompiledStatement.ParsedExoCompute p = CompiledStatement.exoComputeParser(in);

        if (in.hasNextLine()) {
            in.nextLine();
        }

        // then just print the same damn thing out again
        out.print(CBuiltinFunctions.EXO_COMPUTE_NAME.toUpperCase() + " EXOID " + Integer.toString(p.exoId) + " INPUTS [ ");
        compileExoLL(p.inVarsStr);

        out.print("] OUTPUTS [ ");
        compileExoL(p.outVarsStr);

        out.println("]");
    }

    private void compileExoL(List<String> inL) {
        for (String s : inL) {
            out.print(getConstraintVarName(Integer.parseInt(s)) + " ");
        }
    }

    private void compileExoLL(List<List<String>> inL) {
        for (List<String> thisL : inL) {
            out.print("[ ");
            compileExoL(thisL);
            out.print("] ");
        }
    }

	private void compileRAMPutEnhancedConstraint(Scanner in) {
		String retVar = getConstraintTerm(in.next());
		in.next(); // "inputs"
		in.next(); // "["
		in.next(); // "ADDR"
		String addr = getConstraintTerm(in.next());
		in.next(); // "VALUE"
		String value = getConstraintTerm(in.next());
		in.next(); // "CONDITION"
		String condition = getConstraintTerm(in.next());
		String branch = in.next();
		in.nextLine();

		out.println(CBuiltinFunctions.RAMPUT_ENHANCED_NAME.toUpperCase()
				+ " ADDR " + addr + " VALUE " + value + " CONDITION "
				+ condition + " " + branch + " " + retVar);
	}

	private void compileRAMGetEnhancedConstraint(Scanner in) {
		in.next(); // "VALUE"
		String value = getConstraintTerm(in.next());

		in.next(); // "inputs"
		in.next(); // "["
		in.next(); // "ADDR"
		String addr = getConstraintTerm(in.next());
		// in.next(); // "VALUE"

		in.nextLine();

		out.println(CBuiltinFunctions.RAMGET_ENHANCED_NAME.toUpperCase()
				+ " ADDR " + addr + " VALUE " + value);
	}

	private void compileSplitConstraint(Scanner in) {
		ArrayList<Integer> outs = new ArrayList();
		while (true) {
			String got = in.next();
			try {
				int y = Integer.parseInt(got);
				outs.add(y);
			} catch (Throwable e) {
				break;
			}
		}
		while (true) {
			String got = in.next();
			if (got.equals("[")) {
				break;
			}
		}
		String toSplit = getConstraintVarName(in.nextInt()); // Cannot be a
																// constant.
		String type = in.nextLine().split("//", 2)[1].split("\\s+", 2)[1];

		out.println("SIL " + type + " X " + toSplit + " Y0 " + "V"
				+ outs.get(0));
	}

	/**
	 * Mapping as follows: - Are an integer -> the appropriatve var name (see
	 * getConstraintVarName) - starts with C -> just the substring that follows.
	 * - Otherwise, return as is
	 */
	private String getConstraintTerm(String term) {
		int termVarNum;
		try {
			termVarNum = new Integer(term);
		} catch (Throwable e) {
			termVarNum = -1;
		}
		// convert to variable name
		if (termVarNum < 0) {
			if (term.startsWith("C")) {
				term = term.substring(1);
			}
			// Other cases include operators
		} else {
			term = getConstraintVarName(termVarNum);
		}
		return term;
	}

	/**
	 * In ZAATAR, we allow references to input and output variables everywhere.
	 * 
	 * In GINGER, input variables have to be bound to actual variables at the
	 * start of the computation (we can't do multiplication with constants in
	 * GINGER)
	 */
	private String getConstraintVarName(int varNum) {
		switch (Optimizer.optimizationTarget) {
		case ZAATAR:
			if (outputVariables.contains(varNum)) {
				return "O" + varNum;
			}
			if (inputVariables.containsKey(varNum)) {
				return "I" + varNum;
			}
			return "V" + varNum;
		case GINGER:
			return (outputVariables.contains(varNum) ? "O" : "V") + varNum;
		}
		throw new RuntimeException("Failed to identify constraint variable associated with " + Integer.toString(varNum));
	}

	private void compilePolyConstraint(String variableName, Scanner in) {
		FloatConstant multiplier = FloatConstant
				.valueOf(in.next().substring(1));
		if (!multiplier.getNumerator().equals(BigInteger.ONE)) {
			throw new RuntimeException("Assertion error " + multiplier);
		}
		String multiplierDenom = "";
		if (!multiplier.isOne()) {
			multiplierDenom = multiplier.getDenominator() + " * ";
		}
		in.next(); // *
		in.next();

		switch (Optimizer.optimizationTarget) {
		case GINGER:
			compilePolyConstraint_(in);
			out.print(" - " + multiplierDenom + variableName);
			break;
		case ZAATAR:
			// pA
			in.next();
			out.print("( ");
			compilePolyConstraint_(in);
			out.print(" )");
			// * pB
			in.next(); // *
			in.next(); // (
			out.print(" * ( ");
			compilePolyConstraint_(in);
			out.print(" )");
			// + pC
			in.next(); // *
			in.next(); // (
			out.print(" + ( ");
			compilePolyConstraint_(in);
			// Add in output binding term
			out.print(" - " + multiplierDenom + variableName);
			out.print(" )");
			in.next(); // ")"
			break;
		}

		if (!in.next().equals("]")) {
			throw new RuntimeException("Assertion error");
		}

		out.println();
	}

	/**
	 * Reads off a polynomial expression, ending when the parenthesis nesting
	 * depth is negative one.
	 */
	private void compilePolyConstraint_(Scanner in) {
		int nestingDepth = 0;
		boolean hadPreviousTerm = false;
		while (true) {
			String term = in.next();
			if (term.equals("(")) {
				nestingDepth++;
			}
			if (term.equals(")")) {
				nestingDepth--;
				if (nestingDepth < 0) {
					break;
				}
			}
			if (hadPreviousTerm) {
				out.print(" ");
			}
			out.print(getConstraintTerm(term));
			hadPreviousTerm = true;
		}
	}

	private void compileNonPolyConstraint(String variableName, Scanner in) {
		do {
			String term = in.next();
			if (term.equals("]")) {
				break;
			}
			term = getConstraintTerm(term);
			out.print(term + " ");
		} while (true);
		out.println("- " + variableName);
	}
}
