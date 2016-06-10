#include <stdint.h>

#define SIZE 1024

// given a sequence of bytes, run length encode the string
// and then decode it.

struct In { int in[SIZE]; };
struct Out { int out[SIZE]; };

void compute( struct In *input, struct Out *output ) {
    int i;
    int outp = 0;

    // worst case is no compression, in which case we actually double
    // the size of the string!  Note that we could avoid this with a
    // different encoding strategy, but for our purposes the *performance*
    // of the two strategies is little different, and no one is sweating
    // our Weissman Score here. ;)
    int mid[2*SIZE];
    int midp = 0;
    int data = input->in[0];
    int dcount = 0;

    for (i = 1; i < SIZE; i++) {
        if (input->in[i] == data) {
            dcount++;
        } else {
            mid[midp] = data;
            mid[midp + 1] = dcount;
            midp += 2;
            data = input->in[i];
            dcount = 0;
        }
    }

    // write out the last one
    mid[midp] = data;
    mid[midp + 1] = dcount;

    midp = 0;

    [[buffet::fsm(SIZE)]]
    while (outp < SIZE) {
        data = mid[midp];
        dcount = mid[midp+1];
        midp += 2;

        i = 0;
        do {
            output->out[outp] = data;
            outp++;
            i++;
        } while (i <= dcount);
    }
}
