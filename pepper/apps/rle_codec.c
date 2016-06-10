#include <stdint.h>

#define SIZE 4

// given a sequence of bytes, run length encode the string
// and then decode it.

struct In { int in[SIZE]; };
struct Out { int out[SIZE]; };

void compute( struct In *input, struct Out *output ) {
    int i;
    int j;
    int outp = 0;
    bool skip = 0;

    // worst case is no compression, in which case we actually double
    // the size of the string!  Note that we could avoid this with a
    // different encoding strategy, but for our purposes the *performance*
    // of the two strategies is little different, and no one is sweating
    // our Weissman Score here. ;)
    int mid[2*SIZE] = {0,};
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
    midp += 2;

    for (i = 0; i < 2*SIZE; i += 2) {
        if (skip == 0) {
            data = mid[i];
            dcount = mid[i+1];

            output->out[outp] = data;
            outp++;

            // at max, there could be SIZE elements in a row, i.e.,
            // dcount = SIZE - 1
            for (j = 0; j < SIZE - 1; j++) {
                if (j < dcount) {
                    output->out[outp] = data;
                    outp++;
                }
            }

            // if we've written everything, we're done.
            if (outp == SIZE) { skip = 1; }
        }
    }
}
