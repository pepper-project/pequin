#include <stdint.h>
#include <db.h>
#include <avl_tree.h>

#include "tree_balance.h"

int compute(struct In *input, struct Out *output) {
  tree_t tree;
  tree.root = input->root;
  tree_balance(&tree, input->path_depth, input->tree_path);
  output->root = tree.root;
  return 0;
}
