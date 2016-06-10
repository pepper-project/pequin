#include <stdint.h>

/*
  Matrix multiplication.
 */

//Constants
#define SIZE 3

struct In {
  int16_t A[SIZE][SIZE]; int16_t B[SIZE][SIZE];
};

struct Out {
  int64_t C[SIZE][SIZE];
};

int compute(struct In *input, struct Out *output) {
  int i,j,k;

  for(i = 0; i < SIZE; i++){
    for(j = 0; j < SIZE; j++){
      int64_t C_ij = 0;
      for(k = 0; k < SIZE; k++){
        C_ij += input->A[i][k] * input->B[k][j];
      }
      output->C[i][j] = C_ij;
    }
  }
}
