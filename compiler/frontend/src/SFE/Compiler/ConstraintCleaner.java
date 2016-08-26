package SFE.Compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConstraintCleaner {

  //All whitespace should be removed before running.
  private static Pattern renameZaatar = Pattern.compile("\\A\\Q()*()+(\\E(V\\d+)-(O\\d+)\\)\\z");
  private static Pattern variableRef = Pattern.compile("\\b(V\\d+)\\b");
  /*
   * toContraints sometimes prints out constraints of the form
   *
   * ( ) * ( ) + ( V<num> - O<num> ) (zaatar)
   *
   * but we can remove these.
   *
   * Note: this method should only be called in Zaatar mode.
   */
  public static void cleanupConstraints(String uncleanConstraints, String constraintsFile) {
    try {
      //First pass: Detect such constraints, and form a replacement map (size of map bounded by # designated output variables)
      HashMap<String, String> replacements = new HashMap();

      BufferedReader bufferedReader = new BufferedReader(new FileReader(uncleanConstraints));

      String line = null;
      //Scan until the constraints start
      while ((line = bufferedReader.readLine()) != null) {
        if (line.equals("START_CONSTRAINTS")) {
          break;
        }
      }
      //Look for constraints of the right form
      while ((line = bufferedReader.readLine()) != null) {
        if (line.equals("END_CONSTRAINTS")) {
          break;
        }

        //Remove whitespace
        line = line.replaceAll("\\s","");

        Matcher matcher = renameZaatar.matcher(line);
        if(matcher.find()) {
          replacements.put(matcher.group(1), matcher.group(2));
        }
      }
      bufferedReader.close();

      //Now print out the clean constraints
      
      bufferedReader = new BufferedReader(new FileReader(uncleanConstraints));
      PrintWriter out = new PrintWriter(new File(constraintsFile));

      //Everything is the same until the VARIABLES block
      while ((line = bufferedReader.readLine()) != null) {
        out.println(line);
        if (line.equals("START_VARIABLES")) {
          break;
        }
      }
      while ((line = bufferedReader.readLine()) != null) {
        boolean print = true;
        if (!line.isEmpty()) {
          String varName = line.split("\\s+",2)[0];
          if (replacements.containsKey(varName)) {
            print = false;
          }
        }
        if (print) {
          out.println(line);
        }
        if (line.equals("END_VARIABLES")) {
          break;
        }
      }
      //Scan to the contraints block
      while ((line = bufferedReader.readLine()) != null) {
        out.println(line);
        if (line.equals("START_CONSTRAINTS")) {
          break;
        }
      }
      //Process constraints
      while ((line = bufferedReader.readLine()) != null) {
        //is this constraint one we are removing?
        //Note that multiple output variables may be equal to a variable
        //and in that case we only remove the constraint for one of them
        Matcher match = renameZaatar.matcher(line.replaceAll("\\s", ""));
        if (match.matches() && replacements.containsKey(match.group(1)) && replacements.get(match.group(1)).equals(match.group(2))){
        	continue;
        }

        if (line.equals("END_CONSTRAINTS")) {
          out.println(line);
          break;
        }
        //Make all replacements.
        StringBuffer sb = new StringBuffer();
        Matcher matcher = variableRef.matcher(line);
        while(matcher.find()) {
          String word = matcher.group(1);
          String rep = replacements.get(word);
          if (rep != null) {
            word = rep;
          }
          matcher.appendReplacement(sb, word);
        }
        matcher.appendTail(sb);
        out.println(sb);
      }
      //Write out remaining lines
      while ((line = bufferedReader.readLine()) != null) {
        out.println(line);
      }
      bufferedReader.close();
      out.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}
