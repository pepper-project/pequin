#include <stdint.h>

/*
  Matrix multiplication.
 */

//Constants
#define SIZE 30

typedef int32_t num_t;

struct In {
  num_t A[SIZE][SIZE]; num_t B[SIZE][SIZE];
};

struct Out {
  num_t C[SIZE][SIZE];
};

int compute(struct In *input, struct Out *output) {
  int i,j,k;

  for(i = 0; i < SIZE; i++){
    for(j = 0; j < SIZE; j++){
      num_t C_ij = 0;
      for(k = 0; k < SIZE; k++){
        C_ij += input->A[i][k] * input->B[k][j];
      }
      //Truncate down
      output->C[i][j] = (num_t)C_ij;
    }
  }
}
