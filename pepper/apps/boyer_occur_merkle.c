#include <stdint.h>
#include <db.h>

#include "boyer_occur_merkle.h"

void compute(struct In *input, struct Out *output) {
    uint8_t i;
    uint8_t len = PATTERN_LENGTH;
    for(i = 0; i < ALPHABET_LENGTH; i++) {
        ramput(i, &len);
    }

    for(i = 0; i < PATTERN_LENGTH - 1; i++) {
        uint8_t addr = input->input[i];
        uint8_t data = PATTERN_LENGTH - 1 - i;
        ramput(addr, &data);
    }

    for(i = 0; i < ALPHABET_LENGTH; i++) {
        ramget(&(output->output[i]), i);
    }
}
