#include <stdint.h>
#include <db.h>

#define NUM_BITS_TO_PUT 300
#define NUM_INTS_TO_PUT (NUM_BITS_TO_PUT)/32

struct In {uint32_t array[NUM_INTS_TO_PUT];};
struct Out { hash_t result; };

/*
  Microbenchmark to measure the cost of hashput.
*/
void compute(struct In *input, struct Out *output){
  hashput(&output->result, input);
}
