

void fannkuch_input_gen (mpq_t * input_q, int num_inputs, char *argv[]) {

    int m = 13;

    std::vector<int> permutation(m);
    for(int i = 0; i < m; i++) {
        permutation[i] = i+1;
    }
    std::random_shuffle(permutation.begin(), permutation.end());

    for(int i = 0; i < num_inputs; i++) {
        mpq_set_ui(input_q[i], permutation[i], 1);
    }

}
