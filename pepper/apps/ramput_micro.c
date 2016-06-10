#include <stdint.h>
#include <db.h>

struct In { int value; };
struct Out { void placeholder; };

#define NUMACC 10

/*
  Microbenchmark to measure the cost of ramput 
*/
void compute(struct In *input, struct Out *output){
    int i;
    for (i=0; i<NUMACC; i++) {
        ramput(0, &(input->value));
    }
}
