#include <stdint.h>
#define NUM_MAPPERS 2 
#define NUM_REDUCERS 5

#define SIZE_INPUT 5
#define SIZE_OUTPUT 1

struct In {
  uint32_t input[SIZE_INPUT];
};

struct Out {
  uint32_t output[NUM_REDUCERS*SIZE_OUTPUT];
};

void compute(struct In *input, struct Out *output) {
  int i;
  for(i = 0; i < NUM_REDUCERS; i++){
    output->output[i] = input->input[i];
  }
}
