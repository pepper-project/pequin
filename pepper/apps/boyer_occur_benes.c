#include <stdint.h>

#include "boyer_occur_benes.h"

void compute(struct In *input, struct Out *output) {
    uint8_t i;
    for(i = 0; i < ALPHABET_LENGTH; i++) {
        output->output[i] = PATTERN_LENGTH;
    }

    for(i = 0; i < PATTERN_LENGTH - 1; i++) {
        uint8_t addr = input->input[i];
        output->output[addr] = PATTERN_LENGTH - 1 - i;
    }
}
