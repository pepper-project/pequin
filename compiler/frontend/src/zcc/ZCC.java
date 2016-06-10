package zcc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.ParseException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Scanner;

import SFE.Compiler.ArrayConstant;
import SFE.Compiler.AssignmentStatement;
import SFE.Compiler.CompiledStatement;
import SFE.Compiler.ConstExpression;
import SFE.Compiler.ConstraintCleaner;
import SFE.Compiler.ConstraintWriter;
import SFE.Compiler.Consts;
import SFE.Compiler.DependencyProfile;
import SFE.Compiler.FloatType;
import SFE.Compiler.Function;
import SFE.Compiler.IntType;
import SFE.Compiler.Optimizer;
import SFE.Compiler.Program;
import SFE.Compiler.SFECompiler;
import SFE.Compiler.Statement;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;
import ccomp.parser_hw.CCompiler;

public class ZCC {

  /**
   * The main entry point to the compiler.
   *
   * Checks arguments then calls compile()
   */
  public static void main(String[] args) throws IOException {
    String fileName = null;
    String profile = null;
    String prior = null;
    String job = null;
    String defsFile = null;
    boolean verbose = false;
    boolean bigconstraints = false;
    boolean cstdarithtruncate = false;
    long seed = System.nanoTime();
    int tailoredQCQs = 0; // 0 = untailored. Otherwise, tailoredQCQs-many
    // quadratic correction queries will be used as
    // a tailored scheme.
    int statementBufferSize = 10000; //Default to 10,000 if no argument.
    //default options
    Optimizer.optimizationTarget = Optimizer.Target.GINGER;

    int i = 0;
    for (; i < args.length - 2; i++) {
      String arg = args[i];
      if (arg.startsWith("-T")) {
        tailoredQCQs = new Integer(arg.substring(2));
      } else if (arg.startsWith("-O")) {
        Optimizer.optimizationTarget = Optimizer.Target.valueOf(arg.substring(2));
      } else if (arg.startsWith("-B")) {
        statementBufferSize = new Integer(arg.substring(2));
        if (statementBufferSize < 0) {
          statementBufferSize = Integer.MAX_VALUE;
        }
      } else if (arg.equals("--profile")) {
        i++;
        if (i >= args.length - 2) {
          printUsage();
        } else {
          profile = args[i];
        }
        i++;
        if (i >= args.length - 2) {
          printUsage();
        } else {
          prior = args[i];
        }
      } else if (arg.startsWith("--seed")) {
        if (arg.startsWith("--seed=")) {
          seed = new Long(arg.substring("--seed=".length()));
        } else {
          i++;
          if (i >= args.length - 2) {
            printUsage();
          } else {
            seed = new Long(args[i]);
          }
        }
      } else if (arg.equals("--verbose")) {
        verbose = true;
      } else if (arg.equals("--bigconstraints")) {
        bigconstraints = true;
      } else if (arg.equals("--cstdarithtruncate")) {
        cstdarithtruncate = true;
      } else if (arg.equals("--defs")) {
        i++;
        if (i >= args.length - 2) {
          printUsage();
        } else {
          defsFile = args[i];
        }
      } else {
        printUsage();
      }
    }

    if (i < args.length) {
      job = args[i];
      i++;
    }
    if (i < args.length) {
      fileName = args[i];
      i++;
    }

    // Mandatories
    if (fileName == null || job == null) {
      printUsage();
    }

    compile(fileName, job, tailoredQCQs, profile, prior, seed, statementBufferSize, verbose, bigconstraints, defsFile, cstdarithtruncate);
  }


  /**
   * Performs some compilation job.
   * @param defsFile 
   * @param cstdarithtruncate 
   */
  public static void compile(String fileName, String job, int tailoredQCQs, String profilingFile, String priorCircuit, long seed, int statementBufferSize, boolean verbose, boolean bigconstraints, String defsFile, boolean cstdarithtruncate) throws IOException {

    Function.functionSeedGenerator = new Random(seed);

    //boolean opt = job.contains("o");

    String outFileName = new File(fileName).getName();

    outFileName += "." + Optimizer.optimizationTarget.name();

    String circuitFile = outFileName + ".circuit";

    Statement computation = null;

    boolean outputConstants = false;

    AssignmentStatement.typeCheck = job.contains("t");

    AssignmentStatement.allowBigConstraints = bigconstraints;

    DependencyProfile.printUpdates = verbose;
    
    if (!cstdarithtruncate){
      System.out.println("WARNING: --cstdarithtruncate is disabled, so type errors will warn and arithmetic is not ANSI C compliant");
      AssignmentStatement.typeCheckWarns = true;
    }

    // p stands for profile
    boolean doProfile = job.contains("p");
    if (doProfile) {
      DependencyProfile dp = new DependencyProfile();
      dp.makeProfile(new File(fileName), new File(fileName+".profile"));
      return;
    }

    Optimizer.setFirstPass(false);
    // c stands for compile
    if (job.contains("c")) {
      //If profile is available, read it in.
      if (profilingFile != null) {
        File profile = new File(profilingFile);
        File priorCircuit_ = new File(priorCircuit);

        if (verbose)
          System.out.println("Optimizing previously compiled circuit " + priorCircuit);

        if (verbose)
          System.out.println("Using variable dependencies from profile " + profilingFile);

        // construct compiled statement from circuit file and profiling information
        computation = new CompiledStatement(profile, priorCircuit_);

        if (verbose)
          System.out.println("(Complete.)");

      } else {
        Optimizer.setFirstPass(true);

        Program program = new Program();
        program.setTailoringQCQs(tailoredQCQs);

        // "parse" is a better term
        if (verbose)
          System.out.println("Compiling " + fileName);

        Reader file = new BufferedReader(new FileReader(fileName));

        if (fileName.endsWith(".sfdl")) {
          // compile from sfdl file
          SFECompiler compiler = new SFECompiler(file);
          try {
            compiler.compileProgram(program);
          } catch (ParseException pe) {
            if (verbose) {
              pe.printStackTrace();
            }

            System.err.println("Error in line " + pe.getErrorOffset()
                               + ": " + pe.getMessage());
            System.exit(1);
          }
          
          program.addInputStatements();
          program.addOutputStatements();

          program.toSLPTCircuit(null); // This only partially reduces the
          // circuit to SLPT form, specifically the innards of if statements
          // have their transformation postponed until later in compilation.
          // TODO: For ease of explanation, always do this later in the compilation cycle.
        } else if (fileName.endsWith(".c")) {
          // compile from C file
          CCompiler compiler = new CCompiler(file, cstdarithtruncate);
          // Does its own error checking.
          // call c compiler to compile the program for the first pass.
          compiler.compileProgram(program);
        } else {
          String[] extension = fileName.split("\\.");
          throw new RuntimeException("I don't know how to compile file with extension "+extension[extension.length-1]);
        }

        file.close();

        computation = Program.main.getBody();

        outputConstants = true;
      }
    }

    // w means write circuit
    if (job.contains("w")) { // write circuit
      PrintWriter circuit = new PrintWriter(new FileWriter(circuitFile));
      if (verbose)
        System.out.println("Expanding circuit to file " + circuitFile);

      StatementBuffer sb = new StatementBuffer(statementBufferSize, circuit);

      Program.resetCounter(0);
      Optimizer.initOptimizer();
      AssignmentStatement.doRedundantVarAnalysisForAllVariables = job.contains("r");
      // m means combine expression
      AssignmentStatement.combineExpressions = job.contains("m");
      // f means remove fraction
      AssignmentStatement.removeFractions = job.contains("f");
      // call toAssignmentStatements converts statements to circuit format.
      computation.toAssignmentStatements(sb);
      sb.flush();
      circuit.close();
    }

    String constraintsFile = outFileName + ".spec";
    if (outputConstants) {
      // Write out any constants, needed for the exogenous prover to agree
      // with our compiled version of this computation. Do this only when compiling from .sfdl.

      String consFile = constraintsFile + ".cons";
      if (verbose)
        System.out.println("Writing constant values to " + consFile);
      compileConstants(consFile, defsFile);
    }

    //analysis and output to other forms
    if (job.contains("a")) {
      // tailoring analysis. read circuit back in, write out .dot file.
      /*
      String graphFile = outFileName + ".dot";
      compileMultiplicationGraph(circuitFile, graphFile);

      if (tailoredQCQs > 0) {
      	// Read the graph file back in and analyze connected components
      	QuerySelect.main(new String[] { graphFile, "" + tailoredQCQs });
      }
       */
      
      // read circuit file back in, write out constraints file
      ConstraintWriter cw = new ConstraintWriter(circuitFile);
      if (Optimizer.optimizationTarget == Optimizer.Target.ZAATAR) {
        String uncleanConstraints = constraintsFile+"_tmp";
        if (verbose){
          System.out.println("Writing constraints to "+uncleanConstraints);
        }
        cw.toConstraints(uncleanConstraints);
        if (verbose){
          System.out.println("Cleaning up constraints, result will appear in "+constraintsFile);
        }
        ConstraintCleaner.cleanupConstraints(uncleanConstraints, constraintsFile);
      } else {
        if (verbose){
          System.out.println("Writing constraints to "+constraintsFile);
        }
        cw.toConstraints(constraintsFile);
      }

      // System.out.println("Compilation took "+(System.nanoTime() -
      // nanoTimeNow) /1e9+" seconds");
    }
  }

  private static void compileConstants(String consFile, String defsFile) {
    try {
      PrintWriter out = new PrintWriter(new FileWriter(consFile));
      for (Entry<String, ConstExpression> named_ce : Consts.getDeclaredConsts()) {
        String afterType = "";
        ConstExpression ce = named_ce.getValue();
        String ce_name = named_ce.getKey();
        if (ce instanceof ArrayConstant) {
          if (((ArrayConstant)ce).getValues().isEmpty()) {
            continue;
          }
          out.print("int32_t ");
          afterType = "["+((ArrayConstant)ce).size()+"]";
        } else {
          Type ceType = ce.getType();
          if (ce.getType() instanceof IntType) {
            out.print("int32_t ");
          } else if (ce.getType() instanceof FloatType) {
            out.print("double ");
          } else {
            throw new RuntimeException("No equivalent C++ type for "+ceType);
          }
        }

        out.print(ce_name);
        out.print(afterType);
        out.print(" = ");
        compileConstants_(out, ce);
        out.println();
      }
      
      //Now check if any of the #defines can be copied over (have integer value)
      if (defsFile != null){
        Scanner in = new Scanner(new File(defsFile));
        while(in.hasNext()){
          String def = in.next(); 
          if (def.equals("#define")){
            String name = in.next();
            String value = in.nextLine().trim();
            try {
              int ival = Integer.decode(value);
              out.println("int32_t "+name+" = "+ival);
            } catch (Throwable e){
              //Ignore it.
            }
          } else {
            in.nextLine();
          }
        }
        in.close();
      }
      
      out.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void compileConstants_(PrintWriter out, ConstExpression value) {
    if (value instanceof ArrayConstant) {
      ArrayConstant ac = (ArrayConstant) value;
      Iterator<ConstExpression> i = ac.getValues().iterator();
      out.print("{ ");
      while (i.hasNext()) {
        compileConstants_(out, i.next());
        if (i.hasNext()) {
          out.print(", ");
        }
      }
      out.print("} ");
    } else {
      out.print(value.toString().substring(1)); //remove the "C"
      if (value.getType() instanceof FloatType) {
        out.println(".0"); //to make sure that floating point division is used
      }
    }
  }

  /*
  private static void compileMultiplicationGraph(String circuitFile,
  		String graphFile) throws IOException {
  	PrintWriter out = new PrintWriter(new FileWriter(graphFile));
  	out.println("graph G{");
  	// Write out a .dot file, and run a routine to optimally decide which
  	// qcq implements each assignment statement.
  	{
  		Scanner in = new Scanner(new FileReader(circuitFile));
  		while (in.hasNextLine()) {
  			String line = in.nextLine();
  			compileMultiplicationGraph_line(line, out);
  		}
  		in.close();
  	}
  	out.println("}");
  	out.close();
  }

  private static void compileMultiplicationGraph_line(String line, PrintWriter out) {
  	Scanner in = new Scanner(line);
  	String firstTerm = in.next();
  	if (firstTerm.equals("shortcircuit") || firstTerm.equals("}")) {
  		// Ignore this line.
  		return;
  	}
  	String vName = "V" + firstTerm;
  	out.println("\""+vName+"\"");
  	String type = in.next();
  	if (type.equals("output")) {
  		in.next();
  	} else if (type.equals("gate")) {
  		// done.
  	} else if (type.equals("input")) {
  		return;
  	} else {
  		throw new RuntimeException("Bad format of line " + line);
  	}

  	String operation = in.next();
  	if (operation.equals("poly") || operation.equals("identity")) { // no
  																	// functional
  																	// difference
  		in.next();
  		in.next();
  	} else if (operation.equals("!=")) {
  		String newNode = vName + "$!=";
  		out.println("\""+newNode+"\"");
  		printEdge(out, vName, newNode);
  		return;
  	} else if (operation.equals("<")) {
  		ArrayList<String> ltNodes = new ArrayList();
  		for (int i = 0; i < 140; i++) { // XXX dynamically size the size of
  										// < blocks
  			ltNodes.add(vName + "$<$" + i);
  		}
  		// Complete graph
  		for (int i = 0; i < ltNodes.size(); i++) {
  			out.println("\""+ltNodes.get(i)+"\"");
  			for (int j = 0; j < ltNodes.size(); j++) {
  				printEdge(out, ltNodes.get(i), ltNodes.get(j));
  			}
  		}
  		return;
  	} else {
  		throw new RuntimeException("Unsupported analysis for " + line);
  	}
  	{
  		List<String> edges = getProductsInPoly(out, in, "]");
  		//do nothing.
  	}
  }

  private static List<String> getProductsInPoly(PrintWriter out, Scanner in, String breakOnMatch) {
  	ArrayList<String> toRet = new ArrayList();
  	printEdge(out, ct.factors.get(0), ct.factors.get(1));

  	while(true){
  		String word = in.next();
  		if (word.equals(breakOnMatch)) {
  			break;
  		}
  		int termVarNum;
  		try {
  			termVarNum = new Integer(word);
  		} catch (Throwable e) {
  			termVarNum = -1;
  		}
  		// convert to variable name
  		if (termVarNum < 0) {
  			if (word.startsWith("C")) {
  			} else {
  				switch (word.charAt(0)) {
  				case '*':
  					break;
  				case '+':
  				case '-':
  					terms.add(ct);
  					ct = new ConstraintTerm();
  					break;
  				default:
  					throw new RuntimeException("Not recognized token: "
  							+ word);
  				}
  			}
  		} else {
  			ct.factors.add("V" + word);
  		}
  	}
  }


  private static void printEdge(PrintWriter out, String a, String b) {
  	out.println("\"" + a + "\" -- \"" + b + "\"");
  }
  */


  private static void printUsage() {
    System.err.println("Usage: SFECompiler [-T<mu>] [-O<mode>] [-B<buffer size>] [--profile <.circuit file>] [--seed <seed>] [--bigconstraints] [--verbose] <job> <filename.sfdl>");
    System.err
    .println("\tT<mu>: Use <mu> many quadratic correction queries in GINGER tailored scheme.");
    System.err
    .println("\t\tmu = 0 causes the untailored scheme to be used");
    System.err
    .println("\tjob is a string of the following characters, indicating which steps to perform:");
    System.err.println("\t\tc - compilation (produces .cons file, used in exogenous checking)");
    //System.err.println("\t\to - optimization (does nothing unless w also specified)");
    System.err.println("\t\tm - combine expressions");
    System.err.println("\t\tr - redundant code elimination for all expressions");
    System.err.println("\t\tw - writeout");
    System.err.println("\t\tf - remove fractions from polynomials by multiplying out by appropriate constants");
    System.err.println("\t\ta - analysis. produces .spec and .dot output files");
    System.err.println("\t\tt - type check. Only the first pass should be type checked, because important context information is lost after the first pass.");
    System.err.println("\t-O<mode>: Optimize the circuit for a particular framework. Choices:");
    System.err.println("\t\tGINGER");
    System.err.println("\t\tZAATAR");
    System.err.println("\tprofile: Iterated optimizing compilation requires a path to the .circuit file produced by prior compilation.");
    System.err.println("\tverbose: Print status of compilation (for finding bottlenecks)");
    System.err.println("\tseed: A 64bit integer specifying the random number seed for compilation. Each round of iterated optimizing compilation must use the same seed.");
    System.err.println("\tB: The maximum number of statements to remember (defaults to infinity), higher values lead to better expression combination but a small value (32) is usually good enough.");
    System.err.println("\tbigconstraints: Allow the compiler to perform optimizations which decrease number of constraints, but increase size of those constraints.");
    System.exit(1);
  }

  // ~ Static fields/initializers
  // ---------------------------------------------

}
