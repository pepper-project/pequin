package SFE.Compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;

import SFE.Compiler.PolynomialExpression.PolynomialTerm;
import SFE.Compiler.Operators.DivisionOperator;
import SFE.Compiler.Operators.LessOperator;
import SFE.Compiler.Operators.NotEqualOperator;
import SFE.Compiler.Operators.PlusOperator;
import SFE.Compiler.Operators.TimesOperator;
import ccomp.CBuiltinFunctions;

/**
 * A statement based on an already-compiled .circuit file
 */
public class CompiledStatement extends Statement {
	/*
	 * Fields
	 */
	private File profile;
	private File priorCircuit;
	private HashMap<Integer, LvalExpression> varByNumber;
	private TreeMap<Integer, List<Integer>> killList;

	public CompiledStatement(File profile, File priorCircuit) {
		this.profile = profile;
		this.priorCircuit = priorCircuit;
	}

	/*
	 * Methods
	 */
	public Statement toSLPTCircuit(Object obj) {
		// Compiled .circuit files are already in SLPT form.
		return this;
	}

	public Statement duplicate() {
		return new CompiledStatement(profile, priorCircuit);
	}

	private BufferedReader profileReader;

	/**
	 * Read in the precompiled computation, and simply emit all assignments in
	 * that file.
	 */
	public void toAssignmentStatements(StatementBuffer assignments) {
		varByNumber = new HashMap();
		killList = new TreeMap();

		try {
			// Set up the profile reader
			profileReader = new BufferedReader(new FileReader(profile));
			// Construct the BufferedReader object
			BufferedReader bufferedReader = new BufferedReader(new FileReader(
					priorCircuit));

			String line = null;
			while ((line = bufferedReader.readLine()) != null) {
				toAssignmentStatements_(assignments, line);
			}
			bufferedReader.close();
			profileReader.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void toAssignmentStatements_(StatementBuffer assignments,
			String line) {
		Scanner in = new Scanner(line);

		int varNum = nextInt(in);

		while (!killList.isEmpty() && killList.firstKey() < varNum) {
			List<Integer> toKill = killList.remove(killList.firstKey());
			for (Integer q : toKill) {
				varByNumber.remove(q);
			}
		}

		String type = in.next();
		if (type.equals("input")) {
			parseInputLine(varNum, in, assignments);
			return;
		}

		// Not an input statement. See if this statement is used

		ReferenceProfile rp = getReferenceProfileFor(varNum);

		if (rp == null || !rp.isUsed) {
			// No profiling information or unused = ignore this line.
			return;
		}

		if (type.equals("split")) {
			parseSplitLine(varNum, rp, in, assignments);
			return;
		}
        if (type.equals(CBuiltinFunctions.EXO_COMPUTE_NAME)) {
            parseExoComputeLine(varNum, rp, in, assignments);
            return;
        }
		if (type.equals(CBuiltinFunctions.RAMPUT_ENHANCED_NAME)) {
			parseRamPutEnhancedLine(varNum, rp, in, assignments);
			return;
		}
		if (type.equals(CBuiltinFunctions.RAMGET_ENHANCED_NAME)) {
			parseRamGetEnhancedLine(varNum, rp, in, assignments);
			return;
		}
		if (type.equals(CBuiltinFunctions.RAMPUT_NAME)) {
			parseRamPutLine(varNum, rp, in, assignments);
			return;
		}
		if (type.equals(CBuiltinFunctions.RAMGET_NAME)) {
			parseRamGetLine(varNum, rp, in, assignments);
			return;
		}
		if (type.equals(CBuiltinFunctions.HASHPUT_NAME)) {
			parseHashPutLine(varNum, rp, in, assignments);
			return;
		}
		if (type.equals(CBuiltinFunctions.HASHGET_NAME)) {
			parseHashGetLine(varNum, rp, in, assignments);
			return;
		}
		if (type.equals(CBuiltinFunctions.HASHFREE_NAME)) {
			parseHashFreeLine(varNum, rp, in, assignments);
			return;
		}
		if (type.equals(GenericGetStatement.GENERIC_GET_STATEMENT_STR)) {
			parseGenericGetLine(varNum, rp, in, assignments);
			return;
		}
		if (type.equals(CBuiltinFunctions.ASSERT_ZERO_NAME)) {
			parseAssertZeroLine(varNum, rp, in, assignments);
			return;
		}
		if (type.equals("printf")) {
			parsePrintfLine(varNum, rp, in, assignments);
			return;
		}

		if (type.equals("output")) {
			in.next();
		}

		Expression rhs = parseOperation(in);

		String varname = in.next().substring("//".length());
		Type vartype_ = readType(in);

		emitAssignment(varNum, varname, vartype_, rp, rhs, assignments);

		// Does this assignment kill off any variables? This optimization is NOT
		// SAFE if inlining is performed this round.
		// Better just to let another round of dead code elimination take care
		// of
		// these, if needed.
		/*
		 * for (Integer kill : Optimizer.getKillList(varNum)){ LvalExpression
		 * remove = varByNumber.remove(kill); remove.removeReference(null);
		 * //Remove the anticipatory reference that killing a variable removes
		 * if (!remove.isReferenced() && (remove.getAssigningStatement()
		 * instanceof AssignmentStatement)){ //It was never used.
		 * assignments.callbackAssignment
		 * ((AssignmentStatement)remove.getAssigningStatement()); } }
		 */
	}

	private void parseSplitLine(int varNum, ReferenceProfile rp, Scanner in,
			StatementBuffer assignments) {
		while (true) {
			String token = in.next();
			if (token.equals("[")) {
				break;
			}
		}
		Expression toSplit = parseLvalConst(in.next());
		in.next(); // "]"
		String varname = in.next().substring("//".length()); // of the first
		// bit,
		// i.e. ends
		// with :0
		Type bitwiseEncoding = readType(in);
		emitSplitStatement(varNum, varname, bitwiseEncoding, toSplit, rp,
				assignments);
	}

	private void parseRamPutLine(int varNum, ReferenceProfile rp, Scanner in,
			StatementBuffer assignments) {
		while (true) {
			String token = in.next();
			if (token.equals("[")) {
				break;
			}
		}
		in.next(); // "ADDR"
		ArrayList<LvalExpression> addrs = new ArrayList();
		while (true) {
			String token = in.next();
			if (token.equals("NUM_X")) {
				break;
			}
			addrs.add(varByNumber.get(new Integer(token)));
		}
		int num_bits = parseIntConst(in);
		in.next(); // "X"
		Expression[] bits = new Expression[num_bits];
		for (int i = 0; i < num_bits; i++) {
			bits[i] = parseLvalConst(in.next());
		}
		BitString value = new BitString(
				new RestrictedUnsignedIntType(num_bits), bits);

		Program.resetCounter(varNum); // Force the next statement to have the
		// correct line number
		RamPutStatement ss = new RamPutStatement(addrs, value);
		ss.toAssignmentStatements_NoChangeRef(assignments);
	}

	private void parseRamPutEnhancedLine(int varNum, ReferenceProfile rp,
			Scanner in, StatementBuffer assignments) {
		String returnVarNum = in.next();
		while (true) {
			String token = in.next();
			if (token.equals("[")) {
				break;
			}
		}
		in.next(); // "ADDR"
		Expression addr = parseLvalConst(in.next());
		in.next(); // "VALUE"
		Expression value = parseLvalConst(in.next());

		in.next(); // "CONDITION"
		Expression condition = parseLvalConst(in.next());

		boolean branch = Boolean.parseBoolean(in.next());
		in.next(); // "]"

		LvalExpression returnVar = addVariable(new Integer(returnVarNum),
				"returnVar" + returnVarNum, new BusType(), rp);

		Program.resetCounter(varNum); // Force the next statement to have the
		// correct line number
		RamPutEnhancedStatement ss = new RamPutEnhancedStatement(addr, value,
				condition, branch, returnVar);
		ss.toAssignmentStatements_NoChangeRef(assignments);
	}

    static class ParsedExoCompute {
        final int exoId;
        final List<List<String>> inVarsStr;
        final List<String> outVarsStr;

        ParsedExoCompute(String exoIdStr, List<List<String>> inVarsStr, List<String> outVarsStr) {
            this.exoId = Integer.parseInt(exoIdStr);
            this.inVarsStr = inVarsStr;
            this.outVarsStr = outVarsStr;
        }
    }

    // this lets us use the parser for Dependency Profiling where we're working over an array rather than a Scanner
    static class ArrayIterator<E> implements Iterator<E> {
        private final E[] in;
        private int idx;

        ArrayIterator ( E[] in , int...idx ) {
            this.in = in;
            if (idx.length > 0) { this.idx = idx[0]; }
            else { this.idx = 0; }
        }

        public boolean hasNext() { return in.length > idx; }
        public E next() { return in[idx++]; }
        public void remove() { throw new UnsupportedOperationException("ArrayIterator does not support remove."); }
    }

    static ParsedExoCompute exoComputeParser(Iterator<String> in) {
        parseExoCheckInput(in,"EXOID");

        // id#
        final String exoIdStr = in.next();

        parseExoCheckInput(in,"INPUTS");
        parseExoCheckInput(in,"[");

        // input variables
        final List<List<String>> inVarsStr = parseExoLL(in);

        parseExoCheckInput(in,"OUTPUTS");
        parseExoCheckInput(in,"[");

        // output variables
        final List<String> outVarsStr = parseExoL(in);

        // comment
        parseExoCheckInput(in,"//");
        parseExoCheckInput(in,"exo_compute");
        parseExoCheckInput(in,"#"+exoIdStr);
        parseExoCheckInput(in,"inVectors="+Integer.toString(inVarsStr.size()));
        parseExoCheckInput(in,"outVars="+Integer.toString(outVarsStr.size()));

        return new ParsedExoCompute(exoIdStr,inVarsStr,outVarsStr);
    }

    private void parseExoComputeLine(int varNum, ReferenceProfile rp,
            Scanner in, StatementBuffer assignments) {

        // parse the line
        final ParsedExoCompute p = exoComputeParser(in);

        // parse input and output variable numbers into variables
        final List<List<LvalExpression>> inVars = exoFindVarsLL(p.inVarsStr);
        final List<LvalExpression> outVars = exoFindVarsL(p.outVarsStr);

        Program.resetCounter(varNum);   // force next statement to have the correct line number
        final ExoComputeStatement exo = new ExoComputeStatement(inVars,outVars,p.exoId);
        exo.toAssignmentStatements_NoChangeRef(assignments);
    }

    // find vars corresponding to each element in a list
    private List<LvalExpression> exoFindVarsL(List<String> thisL) {
        final List<LvalExpression> outL = new ArrayList<LvalExpression>(thisL.size());
        for (String s : thisL) {
            final LvalExpression iExp = varByNumber.get(Integer.parseInt(s));
            if (null == iExp) {
                throw new RuntimeException("Could not find variable " + s + " for exo_compute input.");
            }
            outL.add(iExp);
        }
        return outL;
    }

    // find vars in each element of a list of lists
    private List<List<LvalExpression>> exoFindVarsLL(List<List<String>> thisLL) {
        final List<List<LvalExpression>> outLL = new ArrayList<List<LvalExpression>>(thisLL.size());
        for (List<String> thisL : thisLL) {
            outLL.add(exoFindVarsL(thisL));
        }
        return outLL;
    }

    private static void parseExoCheckInput(Iterator<String> in, String expected) {
        final String nxt = in.next();

        if (! expected.equals(nxt)) {
            throw new RuntimeException("Expected " + expected + " in parseExoCheckInput, got " + nxt + ".");
        }
    }

    private static List<String> parseExoL(Iterator<String> in) {
        final List<String> outL = new ArrayList<String>();
        while (true) {
            final String nxt = in.next();
            if ("]".equals(nxt)) {
                break;
            } else {
                outL.add(nxt);
            }
        }
        return outL;
    }

    private static List<List<String>> parseExoLL(Iterator<String> in) {
        final List<List<String>> outList = new ArrayList<List<String>>();
        while (true) {
            final String nxt = in.next();
            if ("]".equals(nxt)) {
                break;
            } else if (! "[".equals(nxt)) {
                throw new RuntimeException("Expected [ or ] in parseExoLL, got " + nxt + ".");
            }

            outList.add(parseExoL(in));
        }

        return outList;
    }

	private void parseRamGetEnhancedLine(int varNum, ReferenceProfile rp,
			Scanner in, StatementBuffer assignments) {
		in.next(); // "VALUE"
		String valueVarNum = in.next();
		while (true) {
			String token = in.next();
			if (token.equals("[")) {
				break;
			}
		}
		in.next(); // "ADDR"
		Expression addr = parseLvalConst(in.next());

		in.next(); // "]"
		String varname = in.next().substring("//".length());
		Type valueType = readType(in);
		LvalExpression target = addVariable(new Integer(valueVarNum), varname,
				valueType, rp);
		Program.resetCounter(varNum); // Force the next statement to have the
		// correct line number
		RamGetEnhancedStatement ss = new RamGetEnhancedStatement(target, addr);
		ss.toAssignmentStatements_NoChangeRef(assignments);
	}

	private int parseIntConst(Scanner in) {
		String next = in.next();
		if (next.charAt(0) != 'C') {
			throw new RuntimeException("Non constant argument: " + next);
		}
		return Integer.parseInt(next.substring(1));
	}

	private void parseRamGetLine(int varNum, ReferenceProfile rp, Scanner in,
			StatementBuffer assignments) {
		while (true) {
			String token = in.next();
			if (token.equals("[")) {
				break;
			}
		}
		in.next(); // "ADDR"
		ArrayList<LvalExpression> addrs = new ArrayList();
		while (true) {
			String token = in.next();
			if (token.equals("NUM_Y")) {
				break;
			}
			addrs.add(varByNumber.get(new Integer(token)));
		}
		int num_bits = parseIntConst(in);
		in.next(); // "Y"
		int[] bits = new int[num_bits];
		for (int i = 0; i < num_bits; i++) {
			// Create these bits:
			bits[i] = in.nextInt();
		}
		in.next(); // "]"
		String varname = in.next().substring("//".length()); // of the first
		// bit,
		// i.e. ends
		// with :0
		emitRamGetLine(addrs, varname, bits, rp, assignments);
	}

	private void emitRamGetLine(ArrayList<LvalExpression> addrs,
			String varname, int[] bitNames, ReferenceProfile rp,
			StatementBuffer assignments) {
		LvalExpression[] bits = new LvalExpression[bitNames.length];
		for (int i = 0; i < bits.length; i++) {
			bits[i] = addVariable(bitNames[i], allButLast(varname, 2)
					+ BitString.BIT_SEPARATOR_CHAR + i, new BooleanType(), rp);
		}

		BitString value = new BitString(new RestrictedUnsignedIntType(
				bits.length), bits);

		Program.resetCounter(bitNames[0]); // Force the next statement to have
		// the
		// correct line number
		RamGetStatement ss = new RamGetStatement(value, addrs);
		ss.toAssignmentStatements_NoChangeRef(assignments);
	}

	private void parseHashPutLine(int varNum, ReferenceProfile rp, Scanner in,
			StatementBuffer assignments) {
		while (true) {
			String token = in.next();
			if (token.equals("[")) {
				break;
			}
		}
		in.next(); // "NUM_HASH_BITS"
		int num_hash_bits = parseIntConst(in);
		in.next(); // "HASH_OUT"
		// Write to these bits
		int[] hashBitNames = new int[num_hash_bits];
		for (int i = 0; i < num_hash_bits; i++) {
			hashBitNames[i] = in.nextInt();
		}
		in.next(); // "NUM_X"
		int num_bits = parseIntConst(in);
		in.next(); // "X"
		Expression[] bits = new Expression[num_bits];
		for (int i = 0; i < num_bits; i++) {
			// Read from these bits
			bits[i] = parseLvalConst(in.next());
		}
		in.next(); // "]"
		String varname = in.next().substring("//".length()); // of the first
		// bit,
		// i.e. ends
		// with :0
		emitHashPutLine(hashBitNames, varname, bits, rp, assignments);
	}

	private void emitHashPutLine(int[] hashBitNames, String varname,
			Expression[] bits, ReferenceProfile rp, StatementBuffer assignments) {

		// Read from these bits.
		BitString value = new BitString(new RestrictedUnsignedIntType(
				bits.length), bits);

		// Write to these bits
		LvalExpression[] hashbits = new LvalExpression[hashBitNames.length];
		for (int i = 0; i < hashbits.length; i++) {
			hashbits[i] = addVariable(hashBitNames[i], allButLast(varname, 2)
					+ BitString.BIT_SEPARATOR_CHAR + i, new BooleanType(), rp);
		}
		BitString hashBits = new BitString(new RestrictedUnsignedIntType(
				hashbits.length), hashbits);

		Program.resetCounter(hashBitNames[0]); // Force the next statement to
		// have
		// the correct line number
		new HashPutStatement(hashBits, value)
				.toAssignmentStatements_NoChangeRef(assignments);
	}

	private void parseAssertZeroLine(int varNum, ReferenceProfile rp,
			Scanner in, StatementBuffer assignments) {
		while (true) {
			String token = in.next();
			if (token.equals("[")) {
				break;
			}
		}
		Expression number = varByNumber.get(in.nextInt());

		Program.resetCounter(varNum); // Force the next statement to have the
		// correct line number
		new AssertZeroStatement(number)
				.toAssignmentStatements_NoChangeRef(assignments);
	}

	private void parseHashGetLine(int varNum, ReferenceProfile rp, Scanner in,
			StatementBuffer assignments) {
		while (true) {
			String token = in.next();
			if (token.equals("[")) {
				break;
			}
		}
		in.next(); // "NUM_HASH_BITS"
		int num_hash_bits = parseIntConst(in);
		in.next(); // "HASH_IN"
		Expression[] hashBits = new Expression[num_hash_bits];
		for (int i = 0; i < num_hash_bits; i++) {
			hashBits[i] = parseLvalConst(in.next());
		}
		in.next(); // "NUM_Y"
		int num_bits = parseIntConst(in);
		in.next(); // "Y"
		int[] bits = new int[num_bits];
		for (int i = 0; i < num_bits; i++) {
			// Create these bits:
			bits[i] = in.nextInt();
		}
		in.next(); // "]"
		String varname = in.next().substring("//".length()); // of the first
		// bit,
		// i.e. ends
		// with :0
		emitHashGetLine(hashBits, varname, bits, rp, assignments);
	}

	private void emitHashGetLine(Expression[] hashBits2, String varname,
			int[] bitNames, ReferenceProfile rp, StatementBuffer assignments) {
		LvalExpression[] bits = new LvalExpression[bitNames.length];
		for (int i = 0; i < bits.length; i++) {
			// Remove BitString.BIT_SEPARATOR_CHAR + "0" from the name
			bits[i] = addVariable(bitNames[i], allButLast(varname, 2)
					+ BitString.BIT_SEPARATOR_CHAR + i, new BooleanType(), rp);
		}
		BitString value = new BitString(new RestrictedUnsignedIntType(
				bits.length), bits);

		BitString hashBits = new BitString(new RestrictedUnsignedIntType(
				hashBits2.length), hashBits2);

		Program.resetCounter(bitNames[0]); // Force the next statement to have
		// the
		// correct line number
		new HashGetStatement(value, hashBits)
				.toAssignmentStatements_NoChangeRef(assignments);
	}

	private void parseGenericGetLine(int varNum, ReferenceProfile rp,
			Scanner in, StatementBuffer assignments) {
		String lookup_type = in.next();
		while (true) {
			String token = in.next();
			if (token.equals("[")) {
				break;
			}
		}
		in.next(); // "NUM_HASH_BITS"
		int num_hash_bits = parseIntConst(in);
		in.next(); // "HASH_IN"
		Expression[] hashBits = new Expression[num_hash_bits];
		for (int i = 0; i < num_hash_bits; i++) {
			hashBits[i] = parseLvalConst(in.next());
		}
		in.next(); // "NUM_Y"
		int num_bits = parseIntConst(in);
		in.next(); // "Y"
		int[] bits = new int[num_bits];
		for (int i = 0; i < num_bits; i++) {
			// Create these bits:
			bits[i] = in.nextInt();
		}
		in.next(); // "]"
		String varname = in.next().substring("//".length()); // of the first
		// bit,
		// i.e. ends
		// with :0
		emitGenericGetLine(lookup_type, hashBits, varname, bits, rp,
				assignments);
	}

	private void parsePrintfLine(int varNum, ReferenceProfile rp, Scanner in,
			StatementBuffer assignments) {
		StringBuffer formatString = new StringBuffer();
		while (true) {
			String token = in.next();
			if (token.equals("inputs")) {
				break;
			}
			formatString.append(token + " ");
		}
		in.next(); // "["

		ArrayList<Expression> args = new ArrayList();
		while (true) {
			String token = in.next();
			if (token.equals("]")) {
				break;
			}
			Expression number = varByNumber.get(new Integer(token));
			args.add(number);
		}

		String formatStr = formatString.toString();
		formatStr = formatStr.substring(1, formatStr.length() - 2); // Trim off
		// quotes
		// and
		// trailing
		// space

		Program.resetCounter(varNum); // Force the next statement to have the
		// correct line number
		new PrintfStatement(formatStr, args)
				.toAssignmentStatements_NoChangeRef(assignments);
	}

	private void emitGenericGetLine(String lookup_type, Expression[] hashBits2,
			String varname, int[] bitNames, ReferenceProfile rp,
			StatementBuffer assignments) {
		LvalExpression[] bits = new LvalExpression[bitNames.length];
		for (int i = 0; i < bits.length; i++) {
			// Remove BitString.BIT_SEPARATOR_CHAR + "0" from the name
			bits[i] = addVariable(bitNames[i], allButLast(varname, 2)
					+ BitString.BIT_SEPARATOR_CHAR + i, new BooleanType(), rp);
		}
		BitString value = new BitString(new RestrictedUnsignedIntType(
				bits.length), bits);

		BitString hashBits = new BitString(new RestrictedUnsignedIntType(
				hashBits2.length), hashBits2);

		Program.resetCounter(bitNames[0]); // Force the next statement to have
		// the
		// correct line number
		new GenericGetStatement(lookup_type, value, hashBits)
				.toAssignmentStatements_NoChangeRef(assignments);
	}

	private void parseHashFreeLine(int varNum, ReferenceProfile rp, Scanner in,
			StatementBuffer assignments) {
		while (true) {
			String token = in.next();
			if (token.equals("[")) {
				break;
			}
		}
		in.next(); // "NUM_HASH_BITS"
		int num_hash_bits = parseIntConst(in);
		in.next(); // "HASH_IN"
		Expression[] hashBits = new Expression[num_hash_bits];
		for (int i = 0; i < num_hash_bits; i++) {
			hashBits[i] = parseLvalConst(in.next());
		}
		in.next(); // "]"
		String varname = in.next().substring("//".length());
		emitHashFreeLine(hashBits, varname, varNum, rp, assignments);
	}

	private void emitHashFreeLine(Expression[] hashBits2, String varname,
			int varNum, ReferenceProfile rp, StatementBuffer assignments) {
		BitString hashBits = new BitString(new RestrictedUnsignedIntType(
				hashBits2.length), hashBits2);

		Program.resetCounter(varNum); // Force the next statement to have the
		// correct line number
		new HashFreeStatement(hashBits)
				.toAssignmentStatements_NoChangeRef(assignments);
	}

	private void parseInputLine(int varNum, Scanner in,
			StatementBuffer assignments) {
		String inputName = in.next().substring("//".length());
		Type vartype_ = readType(in);
		Lvalue lval = new VarLvalue(new Variable(inputName, vartype_), false);
		// Function.getVars().add(lval, false);
		LvalExpression lvalExpr = new LvalExpression(lval); // Function.getVars().getVar(inputName);
		varByNumber.put(varNum, lvalExpr);
		Program.resetCounter(varNum); // Force the next statement to have the
		// correct line number
		InputStatement is = new InputStatement(lvalExpr);
		is.toAssignmentStatements(assignments);
	}

	private Expression parseOperation(Scanner in) {
		Expression rhs = null;

		String operation = in.next();
		in.next();
		in.next();
		if (operation.equals("<")) {
			String left = in.next();
			in.next();
			String right = in.next();
			rhs = new BinaryOpExpression(new LessOperator(),
					parseLvalConst(left), parseLvalConst(right));
			in.next(); // ]
		} else if (operation.equals("!=")) {
			String left = in.next();
			in.next();
			String right = in.next();
			rhs = new BinaryOpExpression(new NotEqualOperator(),
					parseLvalConst(left), parseLvalConst(right));
			in.next(); // ]
		} else if (operation.equals("%") || operation.equals("/")) {
			String left = in.next();
			in.next();
			String right = in.next();
			int mode = operation.equals("%") ? DivisionOperator.REMAINDER
					: DivisionOperator.QUOTIENT;
			rhs = new BinaryOpExpression(new DivisionOperator(mode),
					parseLvalConst(left), parseLvalConst(right));
			in.next(); // ]
		} else if (operation.equals("poly") || operation.equals("identity")) {
			PolynomialExpression pe = parsePoly(in, "]"); // Read until matching
			// "]"

			rhs = simplifyPoly(pe);
		} else {
			throw new RuntimeException("I don't know how to parse operator "
					+ operation);
		}

		return rhs;
	}

	private Expression simplifyPoly(PolynomialExpression pe) {

		// It's tricky to apply gate compacting to gates that have already been
		// compacted.
		// If this polynomial is a simple sum of two lval/consts, or a simple
		// product of two lval/consts, recast it as such
		List<PolynomialTerm> pt = pe.getTerms();
		if (pt.size() == 2) { // a + b
			Expression left = pt.get(0).toLvalConst();
			Expression right = pt.get(1).toLvalConst();
			if (left != null && right != null) {
				return new BinaryOpExpression(new PlusOperator(), left, right);
			}
		}
		if (pt.size() == 1) {
			PolynomialTerm onlyTerm = pt.get(0);
			if (onlyTerm.getNumPolynomialFactors() == 0) { // can't handle
				// polynomial
				// factors in this
				// way
				if (onlyTerm.getDegree() == 1) { // c * a
					return new BinaryOpExpression(new TimesOperator(),
							onlyTerm.constant, onlyTerm.getMonomerFactor(0));
				}
				if (onlyTerm.getDegree() == 2 && onlyTerm.constant.isOne()) { // a
					// *
					// b
					return new BinaryOpExpression(new TimesOperator(),
							onlyTerm.getMonomerFactor(0),
							onlyTerm.getMonomerFactor(1));
				}
			}
		}
		return pe;
	}

	private void emitSplitStatement(int varNum, String varname,
			Type bitwiseEncoding, Expression toSplit, ReferenceProfile rp,
			StatementBuffer assignments) {
		int N = IntType.getBits((IntType) bitwiseEncoding);

		LvalExpression[] lhss = new LvalExpression[N];
		for (int i = 0; i < lhss.length; i++) {
			lhss[i] = addVariable(varNum + i, allButLast(varname, 2)
					+ BitString.BIT_SEPARATOR_CHAR + i, new BooleanType(), rp);
		}

		Program.resetCounter(varNum); // Force the next statement to have the
		// correct line number
		SplitStatement ss = new SplitStatement(bitwiseEncoding, toSplit, lhss);
		ss.toAssignmentStatements_NoChangeRef(assignments);
	}

	private String allButLast(String a, int i) {
		return a.substring(0, a.length() - i);
	}

	private void emitAssignment(int varNum, String varname, Type vartype_,
			ReferenceProfile rp, Expression rhs, StatementBuffer assignments) {
		LvalExpression lvalExpr = addVariable(varNum, varname, vartype_, rp);

		Program.resetCounter(varNum); // Force the next statement to have the
		// correct line number
		AssignmentStatement as = new AssignmentStatement(lvalExpr, rhs);
		as.toAssignmentStatements_NoChangeRef(assignments);
	}

	private LvalExpression addVariable(int varNum, String varname,
			Type vartype_, ReferenceProfile rp) {
		// Perform optimizations without making the number any larger.
		Lvalue lval = new VarLvalue(new Variable(varname, vartype_, vartype_),
				rp.isOutput);
		// Function.getVars().add(lval, false);
		LvalExpression lvalExpr = new LvalExpression(lval); // Function.getVars().getVar(varname);
		varByNumber.put(varNum, lvalExpr);

		// Reference information from profile
		lvalExpr.setUBRefCount(rp.refCount); // Set an upper bound on the number
		// of
		// statements referencing this
		// assignment
		lvalExpr.setKillPoint(rp.killPoint); // Set an upper bound on the number
		// of
		// statements referencing this
		// assignment
		addKill(rp.killPoint, varNum);

		return lvalExpr;
	}

	private void addKill(int killPoint, int varNum) {
		List<Integer> atKill = killList.get(killPoint);
		if (atKill == null) {
			atKill = new ArrayList();
			killList.put(killPoint, atKill);
		}
		atKill.add(varNum);
	}

	/**
	 * Represents a line in the .profile file
	 */
	private static class ReferenceProfile {
		public int lineNumber;
		public boolean isUsed;
		public boolean isOutput; // real output - i.e. the final assignment to
		// an
		// output variable
		public int refCount;
		public int killPoint; // When to kill this variable
	}

	private ReferenceProfile lookaheadRef;

	private ReferenceProfile getReferenceProfileFor(int varNum) {
		if (lookaheadRef != null) {
			if (lookaheadRef.lineNumber > varNum) {
				return null;
			}
			if (lookaheadRef.lineNumber == varNum) {
				ReferenceProfile rmLookahead = lookaheadRef;
				lookaheadRef = null;
				return rmLookahead;
			}
		}

		String line = null;
		try {
			while ((line = profileReader.readLine()) != null) {
				String[] line_ = line.split("\\s+");
				ReferenceProfile test = new ReferenceProfile();
				test.lineNumber = Integer.parseInt(line_[0]);
				test.isUsed = line_[1].equals("1");
				test.isOutput = line_[2].equals("1");
				test.refCount = Integer.parseInt(line_[3]);
				test.killPoint = Integer.parseInt(line_[4]);
				if (test.lineNumber == varNum) {
					return test;
				}
				if (test.lineNumber > varNum) {
					lookaheadRef = test;
					return null;
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	private static Type readType(Iterator<String> in) {
		String vartype = in.next();
		if (vartype.equals("float")) {
			in.next();
			int na = nextInt(in);
			in.next();
			int nb = nextInt(in);
			return new RestrictedFloatType(na, nb);
		} else if (vartype.equals("int")) {
			in.next();
			int na = nextInt(in);
			return new RestrictedSignedIntType(na);
		} else if (vartype.equals("uint")) {
			in.next();
			int na = nextInt(in);
			return new RestrictedUnsignedIntType(na);
		} else {
			throw new RuntimeException("Not yet implemented");
		}
	}

	private static int nextInt(Iterator<String> in) {
		return Integer.parseInt(in.next());
	}

	private PolynomialExpression parsePoly(Scanner in, String breakOnMatching) {
		PolynomialExpression pe = new PolynomialExpression();
		PolynomialTerm term = new PolynomialTerm();
		boolean isEmpty = true;
		while (true) {
			String string = in.next();
			if (string.equals(breakOnMatching)) {
				break;
			}
			if (string.equals("+")) {
				// add term to pe
				pe.addMultiplesOfTerm(IntConstant.ONE, term);
				// Create a new term
				term = new PolynomialTerm();
			} else if (string.equals("*")) {
				// No new term.
			} else if (string.equals("(")) {
				PolynomialExpression subPoly = parsePoly(in, ")"); // Reads
				// until
				// matching
				// ")"
				ConstExpression asConst = ConstExpression
						.toConstExpression(subPoly);
				LvalExpression asLval = LvalExpression
						.toLvalExpression(subPoly);
				if (asConst != null) {
					term.addFactor(asConst);
				} else if (asLval != null) {
					term.addFactor(asLval);
				} else {
					term.addFactor(subPoly);
				}
				isEmpty = false;
			} else if (string.equals("-")) {
				term.addFactor(IntConstant.NEG_ONE);
			} else {
				// add factor to term, or set the constant
				term.addFactor(parseLvalConst(string));
				isEmpty = false;
			}
		}
		if (isEmpty) {
			// Special: ( ) = 0.
		} else {
			// add term to pe
			pe.addMultiplesOfTerm(IntConstant.ONE, term);
		}
		return pe;
	}

	/**
	 * Gets the value of the identified variable as an lval/const.
	 */
	private Expression parseLvalConst(String term) {
		if (term.startsWith("C")) {
			return FloatConstant.valueOf(term.substring(1));
		}
		// Don't inline fractional constants when they could potentially end up
		// in
		// nested polynomials
		// (workaround until the "max denominator" detection works for nested
		// polynomials)
		LvalExpression original = varByNumber.get(new Integer(term));
		if (original == null) {
			throw new RuntimeException("No entry for term " + term);
		}
		FloatConstant asConst = FloatConstant.toFloatConstant(original);
		if (asConst != null) {
			if (asConst.getDenominator().equals(BigInteger.ONE)) {
				return asConst;
			}
		}
		// Inline lvals
		AssignmentStatement as = AssignmentStatement.getAssignment(original);
		if (as != null) {
			for (Expression q : as.getAllRHS()) {
				if (q instanceof LvalExpression) {
					return q;
				}
			}
		}
		return original;
	}
}
