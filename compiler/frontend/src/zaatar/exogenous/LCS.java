package zaatar.exogenous;



public class LCS {
  public static char[] DNA = new char[] {'A','T','C','G'};
  public static void main(String[] args) {
    int m = 16;
    int n = 13;
    char[] A = new char[m];
    randomDNA(A);
    char[] B = new char[n];
    randomDNA(B);
    String LCS = algC(0, m-1, A, 0, n-1, B);
    String fastLCS = fastLCS(B,A); //Requires that the m <= n.
    String actLCS = n2LCS(A, B);
    System.out.println(new String(A)+" "+new String(B)+" "+LCS+" "+actLCS+" "+fastLCS);
  }
  private static String fastLCS(char[] A, char[] B) {
    int m = A.length;
    int n = B.length;
    int[][] LL = new int[m][n];
    int[][] choice = new int[m][n];
    char[] LCS = new char[m]; //min(m,n) = m.
    boolean inserted;
    for(int i = m-1; i >= 0; i--) {
      for(int j = n-1; j>=0; j--) {
        if (A[i] == B[j]) {
          LL[i][j] = combine(getScore(LL, i+1, j+1),1);
          choice[i][j] = 0;
        } else {
          int down = getScore(LL, i+1, j);
          int right = getScore(LL, i, j+1);
          if (Math.abs(down - right) > 1) {
            throw new RuntimeException("Assertion Error");
            //They differ by at most one.
          }
          if (down == right + 1) {
            LL[i][j] = down;
            choice[i][j] = 1;
          } else { //same, or right == down+1
            LL[i][j] = right;
            choice[i][j] = 2;
          }
        }
      }
    }

    //Now construct LCS, allowing LCS to contain intermittent zero characters
    int iPtr = 0;
    int jPtr = 0; //Pointers to the element of LL that indicate where the LCS currently ends
    for(int i = 0; i < m; i++) {
      LCS[i] = 0;
      for(int j = 0; j < n; j++) {
        if (i == iPtr && j == jPtr) { //Loop until we meet up with the iPtr and jPtr
          if (choice[i][j] == 0) { //we made a diagonal jump here
            LCS[i] = A[i];
            iPtr = iPtr + 1;
            jPtr = jPtr + 1;
          } else {
            if (choice[i][j] == 1) { //jump down
              iPtr = iPtr + 1;
            } else { //jump right
              jPtr = jPtr + 1;
            }
          }
        }
      }
    }
    //In fact, at this point, LCS[i] is either 0 or equal to A[i], and LCS[i] == A[i] iff. A[i] \in LCS(A,B).

    //Now move any zero character in LCS to the end.
    for(int i = 1; i <= m-1; i++) {
      //Assume LCS[0...i-1] has only trailing zero characters.
      inserted = false;
      for(int j = 0; j <= i-1; j++) {
        if (LCS[j] == 0 && !inserted) {
          //Swap LCS[j] and LCS[i].
          LCS[j] = LCS[i];
          LCS[i] = 0; //TODO make 0
          inserted = true;
        }
      }
      //If we didn't insert, then LCS[i] is in the right position. Leave it be.
    }

    String toRet = new String(LCS).trim(); //Trim does remove \0's!

    //Return the result
    return toRet;
  }
  private static String n2LCS(char[] A, char[] B) {
    int m = A.length;
    int n = B.length;
    int[][] LL = new int[m][n];
    int[][] choice = new int[m][n];
    for(int i = m-1; i >= 0; i--) {
      for(int j = n-1; j>=0; j--) {
        if (A[i] == B[j]) {
          LL[i][j] = combine(getScore(LL, i+1, j+1),1);
          choice[i][j] = 0;
        } else {
          int down = getScore(LL, i+1, j);
          int right = getScore(LL, i, j+1);
          if (down > right) {
            LL[i][j] = down;
            choice[i][j] = 1;
          } else {
            LL[i][j] = right;
            choice[i][j] = 2;
          }
        }
      }
    }

    String result = "";
    int i = 0;
    int j = 0;
    while(!(i == m || j == n)) {
      switch(choice[i][j]) {
      case 0:
        result += A[i];
        i++;
        j++;
        break;
      case 1:
        i++;
        break;
      case 2:
        j++;
        break;
      }
    }
    return result;
  }
  private static int combine(int score, int i) {
    if (score == Integer.MAX_VALUE || i == Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return score + i;
  }
  private static int getScore(int[][] LL, int i, int j) {
    if (i < 0 || i >= LL.length) {
      return 0;
    }
    if (j < 0 || j >= LL[0].length) {
      return 0;
    }
    return LL[i][j];
  }
  private static int[] algB(int istart, int iend, char[] A, int jstart, int jend, char[] B, boolean reverse) {
    int[] K0 = new int[B.length];
    int[] K1 = new int[B.length];
    if (!reverse) {
      for(int i = istart; i <= iend; i++) {
        System.arraycopy(K1, 0, K0, 0, K1.length);
        for(int j = jstart; j <= jend; j++) {
          if (A[i] == B[j]) {
            K1[j] = getScore(K0,j-1) + 1;
          } else {
            K1[j] = Math.max(getScore(K1, j-1),K0[j]);
          }
        }
      }
    } else {
      for(int i = iend; i >= istart; i--) {
        System.arraycopy(K1, 0, K0, 0, K1.length);
        for(int j = jend; j >= jstart; j--) {
          if (A[i] == B[j]) {
            K1[j] = getScore(K0,j+1) + 1;
          } else {
            K1[j] = Math.max(getScore(K1, j+1),K0[j]);
          }
        }
      }
    }
    //System.out.println(new String(A, istart, iend-istart+1) +" "+ new String(B, jstart, jend-jstart+1)+" "+Arrays.toString(K1));
    return K1;
  }
  //Note: istart must be a power of two and iend must be one less than a power of two,
  //OR istart == iend.
  private static String algC(int istart, int iend, char[] A, int jstart, int jend, char[] B) {
    if (jstart > jend) {
      return "";
    }

    //System.out.println(istart+" "+iend+" "+jstart+" "+jend);
    if (istart == iend) {
      //One character base case.
      for(int j = jstart; j <= jend; j++) {
        if (B[j] == A[istart]) {
          return new String(A[istart]+"");
        }
      }
      return "";
    }
    int midI = (istart + iend + 1)/2 - 1;//0-7->3, works
    int[] L1 = algB(istart, midI, A, jstart, jend, B, false);
    int[] L2 = algB(midI+1, iend, A, jstart, jend, B, true);
    //Find first k such that L1[k] + L2[k] is maximal
    int best = -1;
    int splitB = -1;
    for(int k= jstart-1; k <= jend; k++) {
      int sum = 0;
      if (k >= jstart) {
        sum += L1[k];
      }
      if (k < jend) {
        sum += L2[k+1];
      }
      if (sum > best) {
        best = sum;
        splitB = k;
      }
    }
    return algC(istart, midI, A, jstart, splitB, B) + algC(midI+1, iend, A, splitB + 1, jend, B);
  }
  private static int getScore(int[] K, int i) {
    if (i < 0 || i >= K.length) {
      return 0;
    }
    return K[i];
  }
  private static void randomDNA(char[] b) {
    for(int i = 0; i < b.length; i++) {
      b[i] = DNA[(int)(Math.random()*4)];
    }
  }
}
