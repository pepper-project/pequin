package util;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.util.Scanner;

/**
 * Pass in a directory of .cpp files, this looks for lines of the form
 * 	//parsing (quadratic form constraint)
 * and extracts the number of nonzero elements in the A, B, and C matrices.
 */
public class CountNzABC {
  public static void main(String[] args) {
    for(String filename : args) {
      CountNzABC co = new CountNzABC();
      File directory = new File(filename);
      for(File q : directory.listFiles()) {
        co.count(q.toString());
      }
    }
  }

  private int nzABC;
  private void count(String file) {
    System.out.println("Counting file: "+file);
    nzABC = 0;
    try {
      Scanner in = new Scanner(new File(file));
      while(in.hasNextLine()) {
        String line = in.nextLine().trim();
        if (line.contains("// parsing")) {
          countLine(line);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    System.out.println("Nz(A,B,C) = "+nzABC);
  }

  private void countLine(String line) {
    if (line.contains("<") || line.contains("!=")) {
      return;
    }

    Scanner in = new Scanner(line);
    in.next();
    in.next();
    String term = "";
    while(in.hasNext()) {
      String nex = in.next();
      if (nex.equals("sub-constraint")) {
        continue;
      }
      if (nex.equals("+") || nex.equals("-")) {
        addTerm(term);
        term = "";
      } else {
        term += nex+" ";
      }
    }
    addTerm(term);
  }

  private void addTerm(String term) {
    Scanner in = new Scanner(term);
    in.useDelimiter("[ \\*]+");
    boolean hasVariable = false;
    boolean hasConstant = false;
    while(in.hasNext()) {
      String nex = in.next();
      if (nex.startsWith("O") || nex.startsWith("I") || nex.startsWith("V")) {
        nzABC++;
        hasVariable = true;
      } else {
        new BigInteger(nex);
        hasConstant = true;
      }
    }
    //Count constant terms
    if (!hasVariable && hasConstant) {
      nzABC++;
    }
  }

  private static void printUsage() {
    System.out.println("Usage: java "+CountNzABC.class+" <directory of application main cpp's>");
  }
}
