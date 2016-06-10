#include <binary_tree_int_hash_t.h>

struct In {
    tree_t t;
    int k;
	hash_t v;
};

struct Out {
   tree_t t;
   int success;
};

void compute(struct In *input, struct Out *output){
  output->success = tree_insert(&(input->t), input->k, input->v);
  output->t = input->t;
}
