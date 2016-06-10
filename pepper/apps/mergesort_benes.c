#include <stdint.h>

#define MAX_SIZE 8

#define innerLoop(src,dst)                                                                      \
            for (i=lPtr; i<ePtr; i++) {                                                         \
                if ( (lPtr < mPtr) && ( (! (rPtr < ePtr)) || (src[lPtr] < src[rPtr]) ) ) {      \
                    dst[i] = src[lPtr];                                                         \
                    lPtr++;                                                                     \
                } else {                                                                        \
                    dst[i] = src[rPtr];                                                         \
                    rPtr++;                                                                     \
                }                                                                               \
            }

struct In { uint32_t input[MAX_SIZE]; } ;
struct Out { uint32_t output[MAX_SIZE]; } ;

void compute(struct In *input, struct Out *output) {
    int bPtr, ePtr, mPtr, lPtr, rPtr, span;
    int i;
    bool out2in = 0;

    for (span = 1; span < MAX_SIZE; span *= 2) {
        // MAX_SIZE had better be a power of 2!!!

        for (bPtr = 0; bPtr < MAX_SIZE; bPtr += 2*span) {
            lPtr = bPtr;
            mPtr = lPtr + span;
            rPtr = mPtr;
            ePtr = rPtr + span;

            // since loops get unrolled at compile time, these branches do not appear in the circuit
            if (out2in) {
                innerLoop(output->output,input->input)
            } else {
                innerLoop(input->input,output->output)
            }
        }

        out2in = ! out2in;
    }

    if (!out2in) {  // note, !out2in here because it was negated just above
        for (i=0; i<MAX_SIZE; i++) {
            output->output[i] = input->input[i];
        }
    }
}
