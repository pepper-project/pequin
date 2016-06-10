#include <stdint.h>

#define NDEEP 8 
#define NELMS 8 

struct In { uint32_t input[NELMS]; };
struct Out { uint32_t value; };

void compute(struct In *input, struct Out *output) {
    uint32_t i;
    uint32_t current;

    current = input->input[0];

    for (i = 0; i < NDEEP; i++) {
        current = input->input[current];
    }

    output->value = current;
}
