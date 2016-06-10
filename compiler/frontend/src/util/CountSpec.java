package util;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Scanner;

public class CountSpec {
  public static void main(String[] args) {
    if (args.length == 0) {
      printUsage();
    }
    for(String filename : args) {
      CountSpec co = new CountSpec();
      File directory = new File(filename);
      if (!directory.exists()) {
        continue;
      }
      File[] files = directory.listFiles();
      Arrays.sort(files);
      for(File q : files) {
        if (q.isDirectory()) {
          continue;
        }
        co.count(q.toString());
      }
    }
  }

  private HashMap<String, Integer> operations;

  private void count(String file) {
    operations = new HashMap();

    System.out.println("Counting file: "+file);
    try {
      Scanner in = new Scanner(new File(file));

      /*
      	while(in.hasNextLine()){
      		String line = in.nextLine().trim();
      		if (line.startsWith("START_INPUT")){
      			break;
      		}
      	}
      	while(in.hasNextLine()){
      		String line = in.nextLine().trim();
      		if (line.startsWith("END_INPUT")){
      			break;
      		}
      		increment("s");
      	}
      	while(in.hasNextLine()){
      		String line = in.nextLine().trim();
      		if (line.startsWith("START_OUTPUT")){
      			break;
      		}
      	}
      	while(in.hasNextLine()){
      		String line = in.nextLine().trim();
      		if (line.startsWith("END_OUTPUT")){
      			break;
      		}
      		increment("s");
      	}
      	*/
      while(in.hasNextLine()) {
        String line = in.nextLine().trim();
        if (line.startsWith("START_VARIABLES")) {
          break;
        }
      }
      while(in.hasNextLine()) {
        String line = in.nextLine().trim();
        if (line.startsWith("END_VARIABLES")) {
          break;
        }
        increment("n'");
      }
      while(in.hasNextLine()) {
        String line = in.nextLine().trim();
        if (line.startsWith("START_CONSTRAINTS")) {
          break;
        }
      }
      while(in.hasNextLine()) {
        String line = in.nextLine().trim();
        if (line.startsWith("END_CONSTRAINTS")) {
          break;
        }
        increment("chi");
        countConstraint(line);
      }

      System.out.println("n' = " + (get("n'")));
      System.out.println("chi = " + (get("chi")));
      System.out.println("n'+chi+1 = " + (get("n'") + get("chi") + 1));
      System.out.println("#adds = " + (get("add")));
      System.out.println("#muls = " + (get("mul")));
      System.out.println("#!= = " + (get("!=")));
      System.out.println("#< = " + (get("<")));
      System.out.println("Nz(A,B,C) = " + (get("Nz")));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void countConstraint(String line) {
    line = line.replaceAll("\\s*\\(\\s*\\)\\s*\\*\\s*\\(\\s*\\)\\s*\\+\\s*","");

    if (line.contains("!=")) {
      increment("!=");
      return;
    }
    if (line.contains("<")) {
      increment("<");
      return;
    }

    Scanner in = new Scanner(line);
    countConstraint_poly(in);
    //We overcount on additions by one (because of the output variable):
    decrement("add");
  }

  private void countConstraint_poly(Scanner in) {
    boolean hasTerm = false;
    boolean isPolyTerm = false;
    int terms = 0;
    loop: while(in.hasNext()) {
      String next = in.next();
      switch(next.charAt(0)) {
      case '(':
        countConstraint_poly(in);
        isPolyTerm = true;
        hasTerm = true;
        break;
      case ')':
        break loop;
      case '*': //add to current term.
        increment("mul");
        break;
      case '+':
      case '-':
        if (hasTerm) {
          if (!isPolyTerm) {
            increment("Nz");
          }
          terms++;
          hasTerm = false;
          isPolyTerm = false;
        }
        break;
      default:
        hasTerm = true;
      }
    }
    if (hasTerm) {
      if (!isPolyTerm) {
        increment("Nz");
      }
      terms++;
      hasTerm = false;
      isPolyTerm = false;
    }
    for(int k = 0; k < terms - 1; k++) {
      //1) terms - 1 many additions
      increment("add");
    }
  }

  private int get(String str) {
    Integer toRet = operations.get(str);
    if (toRet==null) {
      return 0;
    }
    return toRet;
  }

  private void increment(String str) {
    Integer count = operations.get(str);
    if (count != null) {
      count++;
    } else {
      count = 1;
    }
    operations.put(str, count);
  }
  private void decrement(String str) {
    Integer count = operations.get(str);
    if (count != null) {
      count--;
    } else {
      throw new RuntimeException();
    }
    operations.put(str, count);
  }

  private static void printUsage() {
    System.out.println("Usage: java "+CountSpec.class+" <directory of spec files>");
  }
}
