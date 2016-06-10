#include <stdint.h>

#define ALPHABET_LENGTH 26
#define PATTERN_LENGTH 4
#define HAYSTACK_LENGTH 256

struct In { uint8_t needle[PATTERN_LENGTH]; uint8_t haystack[HAYSTACK_LENGTH]; };
struct Out { uint8_t output; };

void compute(struct In *input, struct Out *output) {
    uint8_t i, j;
    uint8_t table[ALPHABET_LENGTH];
    uint8_t hoff = 0;
    uint8_t last = PATTERN_LENGTH - 1;
    uint8_t done = 0;

    for(i = 0; i < ALPHABET_LENGTH; i++) {
        table[i] = PATTERN_LENGTH;
    }

    for(i = 0; i < last; i++) {
        uint8_t addr = input->needle[i];
        table[addr] = last - i;
    }

    output->output = HAYSTACK_LENGTH;
    for(j = 0; j < 1 + HAYSTACK_LENGTH - PATTERN_LENGTH; j++) {
        if (done == 0) {
            uint8_t mismatch = 0;

            for (i = 0; i < PATTERN_LENGTH; i++) {
                uint8_t hoff_i = hoff + i;
                if (input->haystack[hoff_i] != input->needle[i]) {
                    mismatch = 1;
                }
            }

            if (mismatch == 0) {
                output->output = hoff;
                done = 1;
            } else {
                uint8_t hoff_last = hoff + last;
                uint8_t rchar = input->haystack[hoff_last];
                uint8_t shift = table[rchar];
                hoff += shift;

                if (hoff + PATTERN_LENGTH > HAYSTACK_LENGTH) {
                    done = 1;
                }
            }
        }
    }
}
