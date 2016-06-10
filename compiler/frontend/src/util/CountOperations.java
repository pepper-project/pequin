package util;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Scanner;


public class CountOperations {
  public static void main(String[] args) {
    for(String filename : args) {
      CountOperations co = new CountOperations();
      File directory = new File(filename);
      for(File q : directory.listFiles()) {
        co.count(q.toString());
      }
    }

  }

  private HashMap<String, Integer> operations;

  private void count(String file) {
    System.out.println("Counting file: "+file);
    try {
      Scanner in = new Scanner(new File(file));
      operations = new HashMap();
      while(in.hasNextLine()) {
        String line = in.nextLine().trim();
        if (line.contains("compute_assignment_vectors")) {
          break;
        }
        for(String word : line.split("[( ]")) {
          if (word.startsWith("mpq_") || word.startsWith("mpz_")) {
            increment(word);
          }
        }
      }
      for(Entry<String, Integer> op : operations.entrySet()) {
        System.out.println("#"+op.getKey()+" = "+op.getValue());
      }

      System.out.println("T1' = " + (get("mpq_div") + get("mpq_mul")));
      System.out.println("T2' = " + (get("mpq_sub") + get("mpq_add")));
      System.out.println("T3' = " + (get("mpz_invert")));
    } catch (IOException e) {
      throw new RuntimeException(e);
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

  private static void printUsage() {
    System.out.println("Usage: java "+CountOperations.class+" <directory of prover cpp's>");
  }
}
