#include <stdint.h>
#define SIZE 5
struct In { int32_t A[SIZE][SIZE]; int32_t B[SIZE][SIZE]; };
struct Out { int32_t C[SIZE][SIZE]; };
void compute(struct In *input, struct Out *output){
  int i, j, k;
  for (i=0; i<SIZE; i++) {
    for (j=0; j < SIZE; j++){
      int32_t t = 0;
      for(k = 0; k < SIZE; k++){
        t |= input->A[i][k] & input->B[k][j];
      }
      output->C[i][j] = t;
    }
  }
}
int main(int argc, char **argv){
  struct In input;
  struct Out output;
  compute(&input, &output);
}
