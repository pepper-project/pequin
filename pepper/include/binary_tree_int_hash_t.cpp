#include "binary_tree_int_hash_t.h"
#include "binary_tree.cpp.tmpl"

int _value_equals(tree_value_t* v1, tree_value_t* v2) {
  return hasheq(v1, v2);
}
