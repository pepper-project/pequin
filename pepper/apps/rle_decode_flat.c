#include <stdint.h>

#define OUTPUT_SIZE 10 

// decode a run-length encoded sequence
// format is
//  d l d l d l d l
// where d is the data, l is runlength - 1
// (so  "a0" means a single "a")

// note that in the worst case, the input can be
// twice as long as the output!

struct In { char compressed[2*OUTPUT_SIZE]; };
struct Out { char decompressed[OUTPUT_SIZE]; };

void compute( struct In *input, struct Out *output ) {
    int i;
    int j;
    int outp = 0;

    // every trip through the loop writes at least one value
    // so we only need at max OUTPUT_SIZE trips
    [[buffet::fsm(OUTPUT_SIZE)]]
    for(i = 0; outp < OUTPUT_SIZE; i += 2) {
        int data = input->compressed[i];
        int len = input->compressed[i+1];

        for(j = 0; j <= len; j++) {
            output->decompressed[outp] = data;
            outp++;
            if (outp >= OUTPUT_SIZE) { break; }
        }
    }
}
