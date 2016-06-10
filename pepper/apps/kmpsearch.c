#include <stdint.h>

#define NEEDLE 2 
#define HAYSTACK 8 

struct In { char needle[NEEDLE]; char haystack[HAYSTACK]; };
struct Out { int match; };

void compute(struct In *input, struct Out *output) {
    int k;
    bool skip = 0;

    // construct the failure function
    int fail[NEEDLE];
    fail[0] = -1;
    fail[1] = 0;
    int tpos;
    int cand = 0;
    for(tpos = 2; tpos < NEEDLE; tpos++) {
        char nj = input->needle[tpos - 1];

        // we can jump backwards in the list at most NEEDLE-1 times
        skip = 0;
        for(k = 0; k < NEEDLE-1; k++) {
            if (skip == 0) {
                if (cand > 0 && nj != input->needle[cand]) {
                    cand = fail[cand];
                } else {
                    skip = 1;
                }
            }
        }

        if (cand < 1) {
            fail[tpos] = 0;
        } else {
            cand++;
            fail[tpos] = cand;
        }
    }

    int i = 0;
    int m = 0;
    int j = 0;
    bool skip2 = 0;
    int last = NEEDLE - 1;
    int end = HAYSTACK - last;
    output->match = HAYSTACK;   // if no result is found, return strlen

    // m increments every time we run through this loop, so we cannot
    // have more than end increments, since we can't match past the
    // NEEDLE-1 position in the haystack
    skip = 0;
    for(k = 0; k < end; k++) {
        if (skip == 0) {

            // we could check at most NEEDLE positions
            skip2 = 0;
            for(j = 0; j < NEEDLE; j++) {
                if (skip2 == 0) {
                    if (input->needle[i] == input->haystack[m + i]) {
                        if (i == last) {
                            output->match = m;
                            skip2 = 1;
                            skip = 1;
                        }
                        i++;
                    } else {
                        skip2 = 1;
                    }
                }
            }

            int fi = fail[i];
            if (fi > 0) {
                i = fi;
                fi = fail[i];
                m = m + i - fi;
            } else {
                i = 0;
                m++;
            }
        }
    }
}
