#include <stdint.h>

#define NUMACC 10
#define MEMSIZE 16384

struct In { int in[MEMSIZE]; int addr[NUMACC]; };
struct Out { int out[NUMACC]; };

/*
  Microbenchmark to measure the cost of naive RAM
*/

void compute(struct In *input, struct Out *output){
    int i;
    int j;

    for(i = 0; i < NUMACC; i++) {
        for (j = 0; j < MEMSIZE; j++) {
            if (j == input->addr[i]) {
                output->out[i] = input->in[j];
            }
        }
    }
}
