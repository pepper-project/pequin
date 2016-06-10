#include <avl_tree.h>

struct In {
    tree_t t;
    int lo;
    int hi;
};

struct Out {
   tree_result_set_t r;
};

void compute(struct In *input, struct Out *output){
  tree_find_range(&(input->t), input->lo, TRUE, input->hi, TRUE, &(output->r));
}

