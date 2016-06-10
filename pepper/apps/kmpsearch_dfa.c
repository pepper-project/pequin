#include <stdint.h>

#define NEEDLE 4
#define HAYSTACK 16

struct In { char needle[NEEDLE]; char haystack[HAYSTACK]; };
struct Out { int match; };

void compute(struct In *input, struct Out *output) {
    int k;

    int fail[NEEDLE];
    fail[0] = -1;
    fail[1] = 0;
    int tpos = 2;
    int cand = 0;

    // table can never take more than 2*NEEDLE to construct
    for(k = 0; k < 2*NEEDLE; k++) {
        if (tpos < NEEDLE) {
            if (input->needle[tpos - 1] == input->needle[cand]) {
                cand++;
                fail[tpos] = cand;
                tpos++;
            } else if (cand > 0) {
                cand = fail[cand];
            } else {
                fail[tpos] = 0;
                tpos++;
            }
        }
    }

    output->match = HAYSTACK;
    int i = 0;
    int m = 0;
    int last = NEEDLE - 1;
    int end = HAYSTACK - last;

    // can never perform more than 2*HAYSTACK comparisons
    for(k = 0; k < 2*HAYSTACK; k++) {
        if (m < end) {
            if (input->needle[i] == input->haystack[m+i]) {
                if (i == last) {
                    output->match = m;
                    m = end;    // stop matching
                }
                i++;
            } else {
                int fi = fail[i];
                if (fi > 0) {
                    i = fail[i];
                    fi = fail[i];
                    m = m + i - fi;
                } else {
                    i = 0;
                    m++;
                }
            }
        }
    }
}
