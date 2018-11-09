

void ptrchase_benes_input_gen (mpq_t * input_q, int num_inputs, char *argv[]) {
  for(int i=0; i < num_inputs-1; i++) {
      mpq_set_ui(input_q[i], i+1, 1);
  }
  mpq_set_ui(input_q[num_inputs-1], 0, 1);
}
