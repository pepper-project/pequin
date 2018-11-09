

void pure_hashput_input_gen (mpq_t * input_q, int num_inputs, char *argv[]) {

    for (int i = 0; i < num_inputs; i++) {
        mpq_set_ui(input_q[i], rand(), 1);
    }
}
