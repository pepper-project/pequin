#include <stdint.h>
#include <db.h>
#include "ptrchase_merkle.h"

void compute(struct In *input, struct Out *output) {
    uint32_t i;
    uint32_t current;
    // read in data

    for (i = 0; i < NELMS; i++) {
        ramput(i, &(input->input[i]));
    }

    // start at zero
    ramget(&current, 0);

    for (i = 0; i < NDEEP; i++) {
        uint32_t from = current;
        ramget(&current, from);
    }

    output->value = current;
}
