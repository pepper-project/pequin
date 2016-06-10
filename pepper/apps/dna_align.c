/*
 * Computes the Longest Common Subsequence of two strings, one of 
 * length m and the other of length n in O(m*n) time
 */

//Constants
#define A_LEN 300
//A_LEN >= B_LEN
#define B_LEN 300

int m = A_LEN;
int n = B_LEN;

struct In {
  char A[A_LEN]; char B[B_LEN];
};

//If the LCS is shorter than n characters, the result will be terminated
//with the zero character, i.e. a C-style string is returned.
struct Out {
  char LCS[A_LEN];
};

//Returns 1 iff. 0 <= i < a and 0 <= j < b and 0 o.w.
int checkIndex2(int i, int j, int a, int b){
  return (0 <= i) && (i < a) && (0 <= j) && (j < b);
}

void compute(struct In *input, struct Out *output) {
  int i,j;
  char *A;
  char *B;
  //Dynamic programming memo
  int LL[A_LEN][B_LEN];
  //Hold choices made at each step, for use when backtracking
  int choices[A_LEN][B_LEN]; 
  //Used when backtracking
  int inserted;
  int iPtr, jPtr, diag, down, right;
  char *LCS;

  A = input->A;
  B = input->B;
  LCS = output->LCS;

  //Go backwards from i = m-1 downto 0
  for(i = m-1; i >= 0; i--){
    for(j = n-1; j >= 0; j--){
      if (A[i] == B[j]){
	if (checkIndex2(i+1,j+1,m,n)){
	  diag = LL[i+1][j+1];
	} else {
	  diag = 0;
	}
	//Diagonal jump
	LL[i][j] = 1 + diag;
	choices[i][j] = 0; 
      } else {
	if (checkIndex2(i+1,j,m,n)){
	  down = LL[i+1][j];
	} else {
	  down = 0; 
	}
	if (checkIndex2(i,j+1,m,n)){
	  right = LL[i][j+1];
	} else {
	  right = 0; 
	}
	//Assertion: down and right differ by at most 1
	if (down == right + 1){
	  //Jump down
	  LL[i][j] = down;
	  choices[i][j] = 1;
	} else {
	  //Jump right if down == right or right == down + 1.
	  LL[i][j] = right;
	  choices[i][j] = 2;
	}
      }
    }
  }

  //Construct LCS, allowing it to have intermittent zero characters
  iPtr = 0;
  jPtr = 0; //Pointers to where in LL we are with respect to backtracking
  for(i = 0; i < m; i++){
    LCS[i] = 0; //If A[i] is not in the LCS, this remains 0.
    for(j = 0; j < n; j++){
      if ((i == iPtr) & (j == jPtr)){ //Loop until we meet up with the iPtr and jPtr
	if (choices[i][j] == 0){ //we made a diagonal jump here
	  LCS[i] = A[i];
	  iPtr = iPtr + 1;
	  jPtr = jPtr + 1;
	} else {
	  if (choices[i][j] == 1){//jump down
	    iPtr = iPtr + 1;
	  } else { //jump right
	    jPtr = jPtr + 1;
	  }
	}
      }
    }
  }

  //Now move any string terminator (\0) characters in LCS to the end ala insertion sort
  for(i = 1; i < m; i++){
    inserted = 0;
    for(j = 0; j < i; j++){
      if ((LCS[j] == 0) & !inserted){
	//Swap LCS[j] and LCS[i].
	LCS[j] = LCS[i];
	LCS[i] = 0; 
	inserted = 1;
      }
    }
  }
}
