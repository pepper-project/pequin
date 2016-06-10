#include <stdint.h>
#include <db.h>

/*
  Vector bit rearrangement (shift)
 */

//Constants
#define SIZE 30

struct In {
  uint32_t A[SIZE];
};

struct Out {
  uint32_t B[SIZE];
};

int compute(struct In *input, struct Out *output) {
  int i,j,k;

  for(i = 0; i < SIZE; i++){
    output->B[i] = RROT(input->A[i], 7);
  }
}
