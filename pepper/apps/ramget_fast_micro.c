#include <stdint.h>

#define NUMACC 1024

struct In { void nothing; };
struct Out { int value[NUMACC]; };

/*
  Microbenchmark to measure the cost of ramget
*/

void compute(struct In *input, struct Out *output){
    int i;
    for (i=0; i<NUMACC; i++) {
        ramget_fast(&(output->value[i]), i);
    }
}
