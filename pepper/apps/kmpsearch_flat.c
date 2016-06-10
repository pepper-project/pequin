#include <stdint.h>

#define NEEDLE 16 
#define HAYSTACK 64 

struct In { char needle[NEEDLE]; char haystack[HAYSTACK]; };
struct Out { int match; };

void compute(struct In *input, struct Out *output) {
    int k;

    int fail[NEEDLE];
    fail[0] = -1;
    fail[1] = 0;
    int tpos;
    int cand = 0;

    // table can never take more than 2*NEEDLE to construct
    [[buffet::fsm(NEEDLE * 2)]]
    for(tpos = 2; tpos < NEEDLE; tpos++) {
        char nj = input->needle[tpos - 1];

        while (cand > 0 && nj != input->needle[cand]) {
            cand = fail[cand];
        }

        if (cand < 1) {
            fail[tpos] = 0;
        } else {
            cand++;
            fail[tpos] = cand;
        }
    }

    output->match = HAYSTACK;
    int i = 0;
    int m = 0;
    int last = NEEDLE - 1;
    int end = HAYSTACK - last;

    // can never perform more than 2*HAYSTACK comparisons
    [[buffet::fsm(HAYSTACK * 2)]]
    while (m < end) {
        if (input->needle[i] == input->haystack[m+i]) {
            if (i == last) {
                output->match = m;
                break;
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
