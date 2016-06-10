#include <stdint.h>
#include <db.h>

#define MAX_SIZE 4

struct In { int input[MAX_SIZE]; };
struct Out { int output[MAX_SIZE]; } ;

void compute(struct In *input, struct Out *output) {
    int i;

    // first, copy input into RAM
    for (i = 0; i < MAX_SIZE; i++) {
        ramput(i, &(input->input[i]));
    }

    // now copy output from RAM
    for (i = 0; i < MAX_SIZE; i++) {
        ramget(&(output->output[i]), i);
    }
}
