package ginger.complib;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Automatic tailoring for the GINGER protocol.
 *
 * The multiplicative graph of a computation has a vertex corresponding to any variable in the computation,
 * and has an edge between two vertices V1 and V2 associated with variables Z1 and Z2 whenever Z1 * Z2
 * is a product used in the computation.
 *
 * This routine covers the multiplicative graph of a computation with
 * a fixed number \muQC of complete graphs (i.e. quadratic correction tests),
 * such that the following objective is minimized:
 *    Objective = \sum (|E(D)|), for all D covering complete graphs
 *
 * I do not know an efficient solution for this problem. This class implements a simple heuristic, as follows.
 * First, the input multiplicative graph is split into connected components. Next, these connected components
 * are assigned to \muQC "bins" by either
 *  1) A heuristic - iterate over the components in descending order of size, always add the component to the most
 *  empty bin. This attempts to make the bins hold the same number of vertices.
 *  2) A dynamic programming algorithm which gives the best assignment to the bins to optimize the objective function.
 *  I predict large memory use for this algorithm.
 */
public class QuerySelect {
  /**
   * Usage: pass in the path to a .dot file as the first argument, and the number of qcqs to create as the second
   *
   * The created queries are written to (name).qc .
   */
  public static void main(String[] args) throws FileNotFoundException {
    String dotFile = args[0];
    String qcFile = dotFile.substring(0,dotFile.length()-4)+".qc";
    int numTailoredQCQs = new Integer(args[1]);
    Scanner in = new Scanner(new File(dotFile));
    QuerySelect qs = new QuerySelect();
    in.nextLine(); // graph g{
    while(true) {
      String line = in.nextLine().trim();
      if (line.equals("}")) {
        break;
      }
      if (line.contains("--")) {
        qs.readEdge(line);
      } else {
        qs.readVertex(line);
      }
    }
    in.close();

    qs.process();

    System.out.println("There were "+qs.numEdges+" edges.");
  }

  private void readVertex(String line) {

  }

  private int numEdges = 0;
  private void readEdge(String line) {
    numEdges++;
  }


  private void process() {

  }
}
