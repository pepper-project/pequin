#ifndef BINARY_TREE_INT_HASH_T_H_
#define BINARY_TREE_INT_HASH_T_H_

#define KEY_T int
#define VALUE_T hash_t

#define MAX_TREE_DEPTH 11
#define MAX_TREE_RESULTS 5
#define MAX_TREE_NODE_VALUES 1

#include "binary_tree.h.tmpl"

int _value_equals(tree_value_t* v1, tree_value_t* v2) {
  return hasheq(v1, v2);
}

//#undef KEY_T
//#undef VALUE_T

#endif //BINARY_TREE_INT_HASH_T_H_
