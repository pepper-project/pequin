package util;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;

/**
 * Pass in a directory of .cpp files, this looks for the lines
 * num_cons = \chi
 * and
 * num_vars = m
 */
public class CountVarsAndConstraints {
  public static void main(String[] args) {
    for(String filename : args) {
      CountVarsAndConstraints co = new CountVarsAndConstraints();
      File directory = new File(filename);
      for(File q : directory.listFiles()) {
        co.count(q.toString());
      }
    }
  }

  private void count(String file) {
    System.out.println("Counting file: "+file);
    try {
      Scanner in = new Scanner(new File(file));
      while(in.hasNextLine()) {
        String line = in.nextLine().trim();
        if (line.contains("num_cons = ") || line.contains("num_vars")) {
          System.out.println(line);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void printUsage() {
    System.out.println("Usage: java "+CountVarsAndConstraints.class+" <directory of application main cpp's>");
  }
}
