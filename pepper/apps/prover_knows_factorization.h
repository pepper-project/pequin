#include <stdint.h>

#define NUM_OF_FACTORS_UPPER_BOUND 10

/**
 * Explanation and usage of NUM_OF_FACTORS_UPPER_BOUND
 *
 * Large numbers have more than just 2 factors. The number of factors should be oblivious 
 * to both the program (pepper/apps/prover_knows_factorization.c) and the verifier.
 * Thus, we use an upper bound (NUM_OF_FACTORS_UPPER_BOUND = 10) to the number of factors.
 * If the factors are less than NUM_OF_FACTORS_UPPER_BOUND, the rest should be filled with 1.
**/

struct In {
    uint32_t input_num[1];            			/* public input number */
};

struct Out {
    uint8_t prover_knows_factors; 	        /* boolean output */
};

typedef struct factors { 
    uint32_t factors[NUM_OF_FACTORS_UPPER_BOUND]; 	/* private factors (prover will provide and multiply them) to prove that he knows them */
} factors_t;
