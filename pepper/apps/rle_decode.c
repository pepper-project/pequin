#include <stdint.h>

#define OUTPUT_SIZE 4 

// decode a run-length encoded sequence
// format is
//  d l d l d l d l
// where d is the data, l is runlength - 1
// (so  "a0" means a single "a")

// note that in the worst case, the input can be
// twice as long as the output!

struct In { int compressed[2*OUTPUT_SIZE]; };
struct Out { int decompressed[OUTPUT_SIZE]; };

void compute( struct In *input, struct Out *output ) {
    int i;
    int j;
    int outp = 0;
    bool skip = 0;

    for (i = 0; i < 2*OUTPUT_SIZE; i += 2) {
        if (skip == 0) {
            int data = input->compressed[i];
            int len = input->compressed[i+1];

            // max run length is OUTPUT_SIZE
            for (j = 0; j < (OUTPUT_SIZE); j++) {
                if ( (skip == 0) && (len > -1) ) {
                    output->decompressed[outp] = data;
                    outp++;
                    if (outp >= OUTPUT_SIZE) { skip = 1; }
                    len--;
                }
            }
        }
    }
}
