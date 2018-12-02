#include <prover_knows_factorization.h>

/**
 * Algorithm Description:
 *
 * 1) Verifier sets up the P and V keys:
 *      ./pepper_compile_and_setup_V.sh prover_knows_factorization prover_knows_factorization.vkey prover_knows_factorization.pkey
 *
 * 2) Prover claims that she knows the factors of the large number (located in the exo1 file) Note: exo1 should be executable and placed inside pequin/pepper/bin directory
 *      ./pepper_compile_and_setup_P.sh prover_knows_factorization
 *
 * 3) Verifier generates and provides the number (located in the prover_knows_factorization.inputs file)
 *      bin/pepper_verifier_prover_knows_factorization gen_input prover_knows_factorization.inputs
 *
 * 4) Prover generates a proof (prover_knows_factorization.proof) that he knows the factors
 *      bin/pepper_prover_prover_knows_factorization prove prover_knows_factorization.pkey prover_knows_factorization.inputs prover_knows_factorization.outputs prover_knows_factorization.proof
 *
 * 5) Finally, Verifier checks the computation without ever knowing the factors.
 *      bin/pepper_verifier_prover_knows_factorization verify prover_knows_factorization.vkey prover_knows_factorization.inputs prover_knows_factorization.outputs prover_knows_factorization.proof
**/

void compute(struct In *input, struct Out *output) {
    uint32_t i;
    uint32_t *public_num[1] = { input->input_num };         /* This is the input-number that Prover claims that knows its factorization (provided by pepper/input_generation/prover_knows_factorization_v_inp_gen.h) */
    factors_t factors[1];                                   /* Prover will fill it with his private factors (provided by exo1) */
    uint32_t len[1] = { 1 };                                /* Length of public_num array: 1 number */
    
    exo_compute(public_num, len, factors, 1);               /* Fill public_num vector with prover's private factors */
    
    /* provers_computed_number = f1 * f2 * ... * fn */
    uint32_t provers_computed_number = 1;
    for (i = 0 ; i < NUM_OF_FACTORS_UPPER_BOUND ; i++) {    /* Obliviously multiply until an upper bound */
        provers_computed_number *= factors[0].factors[i];
    }
    
    if (public_num[0][0] == provers_computed_number) {      /* Finally check if the two numbers are equal */
        output->prover_knows_factors = 1;
    } else {
        output->prover_knows_factors = 0;
    }

}
