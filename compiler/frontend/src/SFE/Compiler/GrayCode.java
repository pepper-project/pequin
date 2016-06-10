package SFE.Compiler;


/**
 * Methods are not threadsafe
 */
public class GrayCode {
  public static boolean getBit(int i, int j, int n) {
    boolean result = ((i ^ (i >> 1)) & (1 << (n-1-j))) != 0;
    //System.out.println(i+" "+j+" "+result);
    return result;
  }
}
