#include <stdint.h>
#define SIZE 2
int mat[SIZE*SIZE] = { 0x12, 0x5, 0x3, 0x2 };

struct In { int16_t vector[SIZE]; };
struct Out { int32_t result[SIZE]; };

void compute(struct In *input, struct Out *output){
  int i, j, k, t;
  for (i=0; i<SIZE; i++) {
    int32_t t=0;
    for (k=0; k<SIZE; k++) {
      t = t + mat[i*SIZE+k] * input->vector[k];
    }
    output->result[i] = t;
  }
}

int main(int argc, char **argv){
  struct In input;
  struct Out output;
  //Compute on memory contents? (TODO real initializer)
  compute(&input, &output);
}

