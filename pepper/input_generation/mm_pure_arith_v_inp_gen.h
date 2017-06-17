

void mm_pure_arith_input_gen (mpq_t * input_q, int num_inputs) {
    srand(time(NULL));
    for (int i = 0; i < num_inputs; i++) {
        mpq_set_ui(input_q[i], rand(), 1);
    }
}
