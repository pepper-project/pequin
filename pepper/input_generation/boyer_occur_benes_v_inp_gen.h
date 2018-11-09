#include <apps/boyer_occur_benes.h>

void boyer_occur_benes_input_gen (mpq_t * input_q, int num_inputs, char *argv[]) {


    srand(time(NULL));
    for(int j = 0; j < PATTERN_LENGTH; j++) {
        mpq_set_ui(input_q[j], rand() % ALPHABET_LENGTH, 1);
    }
}
