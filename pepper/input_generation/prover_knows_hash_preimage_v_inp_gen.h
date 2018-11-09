
uint8_t sha256_hash[32] = { 136, 212, 38, 111, 212, 230, 51, 141, 19, 184, 69, 252, 242, 137, 87, 157, 32, 156, 137, 120, 35, 185, 33, 125, 163, 225, 97, 147, 111, 3, 21, 137 };
// sha256 hash of 'a', 'b', 'c', 'd'

void prover_knows_hash_preimage_input_gen (mpq_t * input_q, int num_inputs, char *argv[]) {

    for (int i = 0; i < num_inputs; i++) {
        mpq_set_ui(input_q[i], sha256_hash[i], 1);
    }
}
