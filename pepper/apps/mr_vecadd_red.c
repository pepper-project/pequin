#include <stdint.h>
#define NUM_MAPPERS 2 
#define NUM_REDUCERS 5

#define SIZE_INPUT 2
#define SIZE_OUTPUT 1

struct In {
  uint32_t in[SIZE_INPUT];
};

struct Out {
  uint32_t out;
};

void compute(struct In *input, struct Out *output) {
  int i;
  output->out = 0;
  for(i = 0; i < SIZE_INPUT; i++){
    output->out = output->out + input->in[i];
  }
}
