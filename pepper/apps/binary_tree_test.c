#include <stdint.h>
#include <db.h>
#include <binary_tree.h>

struct In {
	uint32_t in_val;
};

struct Out {
	tree_result_set_t result_set1;
	tree_result_set_t result_set2;
	tree_result_set_t result_set3;
	tree_result_set_t result_set4;
	tree_result_set_t result_set5;
	tree_result_set_t result_set6;
};

void compute(struct In *input, struct Out *output){
  tree_t tree;
  BOOL success = FALSE;

  tree_init(&tree);

  success = tree_insert(&tree, 6, 47);

  success = tree_insert(&tree, 5, 42);
  success = tree_insert(&tree, 5, 43);
  success = tree_insert(&tree, 5, 44);

  tree_remove_value(&tree, 5, 43);

  tree_find_eq(&tree, 5, &(output->result_set1));

  tree_find_eq(&tree, 6, &(output->result_set2));

  success = tree_remove(&tree, 5);

  tree_find_eq(&tree, 5, &(output->result_set3));

  success = tree_insert(&tree, 9, 112);
  success = tree_insert(&tree, 27, 8);
  success = tree_insert(&tree, 63, 789);

  tree_find_lt(&tree, 27, FALSE, &(output->result_set4));

  tree_find_gt(&tree, 6, FALSE, &(output->result_set5));

  tree_find_range(&tree, 9, TRUE, 63, FALSE, &(output->result_set6));
}
