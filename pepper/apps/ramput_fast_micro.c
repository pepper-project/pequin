#include <stdint.h>

#define NUMACC 1024

struct In { int addr[NUMACC]; };
struct Out { void nothing; };

/*
  Microbenchmark to measure the cost of ramget
*/

void compute(struct In *input, struct Out *output){
    int i;
    for (i=0; i<NUMACC; i++) {
        ramput_fast(i, input->addr[i]);
    }
}
