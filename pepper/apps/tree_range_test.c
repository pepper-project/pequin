#include <stdint.h>
#include <db.h>
#include <binary_tree.h>

struct In {
	uint32_t in_val;
};

struct Out {
	tree_result_set_t result_set1;
	tree_result_set_t result_set2;
};

void compute(struct In *input, struct Out *output){
  tree_t tree;
  BOOL success = FALSE;

  tree_init(&tree);

  success = tree_insert(&tree, 9, 112);
  success = tree_insert(&tree, 27, 8);
  success = tree_insert(&tree, 63, 789);

  tree_find_gt(&tree, 26, FALSE, &(output->result_set1));
  tree_find_range(&tree, 8, TRUE, 63, FALSE, &(output->result_set2));
}
