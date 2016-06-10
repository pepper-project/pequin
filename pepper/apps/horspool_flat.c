#include <stdint.h>

#define ALPHABET_LENGTH 26
#define PATTERN_LENGTH 4
#define HAYSTACK_LENGTH 256

struct In { uint8_t needle[PATTERN_LENGTH]; uint8_t haystack[HAYSTACK_LENGTH]; };
struct Out { uint8_t output; };

void compute(struct In *input, struct Out *output) {
    uint8_t i, j;
    uint8_t table[ALPHABET_LENGTH];
    uint8_t last = PATTERN_LENGTH - 1;

    for(i = 0; i < ALPHABET_LENGTH; i++) {
        table[i] = PATTERN_LENGTH;
    }

    for(i = 0; i < last; i++) {
        uint8_t addr = input->needle[i];
        table[addr] = last - i;
    }

    output->output = HAYSTACK_LENGTH;

    int hlen = HAYSTACK_LENGTH - PATTERN_LENGTH;
    int hoff = 0;
    int done = 0;

    [[buffet::fsm(HAYSTACK_LENGTH - ALPHABET_LENGTH + 1)]]
    while (hlen >= 0) {
        int i;
        int hoff_i = hoff + last;
        for(i = last; input->haystack[hoff_i] == input->needle[i]; i--) {
            if (i == 0) {
                done = 1;
                output->output = hoff;
                break;
            }
            hoff_i--;
        }

        if (done) {
            break;
        }

        int hoff_last = hoff + last;
        uint8_t rchar = input->haystack[hoff_last];
        uint8_t shift = table[rchar];
        hlen -= shift;
        hoff += shift;
    }
}
