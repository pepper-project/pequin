#include <stdint.h>
#include <db.h>

#define NUMACC 10

struct In { void placeholder; };
struct Out { int value[NUMACC]; };

/*
  Microbenchmark to measure the cost of ramget
*/
void compute(struct In *input, struct Out *output){
    int i;
    for (i=0; i<NUMACC; i++) {
        ramget(&(output->value[i]), 0);
    }
}
