#include <avl_tree.h>

struct In {
    tree_t t;
    int k;
};

struct Out {
   tree_result_set_t r;
};

void compute(struct In *input, struct Out *output){
  tree_find_eq(&(input->t), input->k, &(output->r));
}

