#ifndef AVL_TREE_H_
#define AVL_TREE_H_

#pragma pack(push)
#pragma pack(1)

#include <stdlib.h>
#include <stdint.h>
#include <include/db.h>

#define KEY_T int
#define VALUE_T hash_t

typedef int BOOL;

#define TRUE 1
#define FALSE 0
#define NULL_PTR NULL_HASH

#define MAX_TREE_DEPTH 12
#define MAX_TREE_RESULTS 5
#define MAX_TREE_NODE_VALUES 1

#define TREE_PATH_LEFT -1
#define TREE_PATH_EQUALS 0
#define TREE_PATH_RIGHT 1

#define TREE_TYPE_IND(type, key_t, value_t) \
  type ## _ ## key_t ## _ ## value_t

#define TREE_TYPE(type, key_t, value_t) \
  TREE_TYPE_IND(type, key_t, value_t)

#define TREE_FUNC_IND(method, key_t, value_t) \
  method ## _ ## key_t ## _ ## value_t

#define TREE_FUNC(method, key_t, value_t) \
  TREE_FUNC_IND(method, key_t, value_t)

#define BINARY_TREE_TMPL_H_(KEY_T,VALUE_T) \
  BINARY_TREE_TMPL_H_ ## KEY_T ## _ ## VALUE_T

#define tree_key_t KEY_T
#define tree_value_t VALUE_T
#define tree TREE_TYPE(avl_tree, KEY_T, VALUE_T)
#define tree_t TREE_TYPE(avl_tree_t, KEY_T, VALUE_T)
#define tree_result TREE_TYPE(avl_tree_result, KEY_T, VALUE_T)
#define tree_result_t TREE_TYPE(avl_tree_result_t, KEY_T, VALUE_T)

#define tree_node TREE_TYPE(avl_tree_node, KEY_T, VALUE_T)
#define tree_node_t TREE_TYPE(avl_tree_node_t, KEY_T, VALUE_T)
#define tree_path TREE_TYPE(avl_tree_path, KEY_T, VALUE_T)
#define tree_path_t TREE_TYPE(avl_tree_path_t, KEY_T, VALUE_T)

#define _value_equals TREE_FUNC(_avl_value_equals, KEY_T, VALUE_T)
#define _rotate_left_left TREE_FUNC(_avl_rotate_left_left, KEY_T, VALUE_T)
#define _rotate_left_right TREE_FUNC(_avl_rotate_left_right, KEY_T, VALUE_T)
#define _rotate_right_right TREE_FUNC(_avl_rotate_right_right, KEY_T, VALUE_T)
#define _rotate_right_left TREE_FUNC(_avl_rotate_right_left, KEY_T, VALUE_T)
#define _copy_value TREE_FUNC(_avl_copy_value, KEY_T, VALUE_T)
#define _replace_value TREE_FUNC(_avl_replace_value, KEY_T, VALUE_T)
#define _copy_result TREE_FUNC(_avl_copy_result, KEY_T, VALUE_T)
#define _copy_results TREE_FUNC(_avl_copy_results, KEY_T, VALUE_T)
#define _find_path TREE_FUNC(_avl_find_path, KEY_T, VALUE_T)
#define _find_first TREE_FUNC(_avl_find_first, KEY_T, VALUE_T)
#define _find_next_below TREE_FUNC(_avl_find_next_below, KEY_T, VALUE_T)
#define _find_next_above TREE_FUNC(_avl_find_next_above, KEY_T, VALUE_T)
#define _find_next TREE_FUNC(_avl_find_next, KEY_T, VALUE_T)
#define _recompute_path TREE_FUNC(_avl_recompute_path, KEY_T, VALUE_T)
#define _recompute_path_no_balance TREE_FUNC(_avl_recompute_path_no_balance, KEY_T, VALUE_T)

#define tree_init TREE_FUNC(avl_tree_init, KEY_T, VALUE_T)
#define tree_find_eq TREE_FUNC(avl_tree_find_eq, KEY_T, VALUE_T)
#define tree_find_lt TREE_FUNC(avl_tree_find_lt, KEY_T, VALUE_T)
#define tree_find_gt TREE_FUNC(avl_tree_find_gt, KEY_T, VALUE_T)
#define tree_find_range TREE_FUNC(avl_tree_find_range, KEY_T, VALUE_T)
#define tree_insert TREE_FUNC(avl_tree_insert, KEY_T, VALUE_T)
#define tree_update TREE_FUNC(avl_tree_update, KEY_T, VALUE_T)
#define tree_insert_no_balance TREE_FUNC(avl_tree_insert_no_balance, KEY_T, VALUE_T)
#define tree_update_no_balance TREE_FUNC(avl_tree_update_no_balance, KEY_T, VALUE_T)

#define tree_remove TREE_FUNC(avl_tree_remove, KEY_T, VALUE_T)
#define tree_remove_value TREE_FUNC(avl_tree_remove_value, KEY_T, VALUE_T)
#define tree_balance TREE_FUNC(avl_tree_balance, KEY_T, VALUE_T)

//typedef uint32_t tree_key_t;
//typedef hash_t tree_value_t;

typedef struct compressed_tree_node {
  uint32_t left;
  uint32_t right;

  tree_key_t key;

  uint8_t num_values;
  uint32_t values[MAX_TREE_NODE_VALUES];

  uint8_t height;
  uint8_t height_left;
  uint8_t height_right;
} compressed_tree_node_t;

typedef struct tree_node {
  hash_t left;
  hash_t right;

  tree_key_t key;

  uint8_t num_values;
  tree_value_t values[MAX_TREE_NODE_VALUES];

  uint8_t height;
  uint8_t height_left;
  uint8_t height_right;
} tree_node_t;

typedef struct tree {
  hash_t root;
} tree_t;

typedef struct tree_result {
  tree_key_t key;
  tree_value_t value;
} tree_result_t;

typedef struct tree_result_set {
  uint8_t num_results;
  tree_result_t results[MAX_TREE_RESULTS];
} tree_result_set_t;

typedef struct tree_path {
  uint8_t depth;
  tree_node_t nodes[MAX_TREE_DEPTH];
  int8_t branches[MAX_TREE_DEPTH];
} tree_path_t;

#define WITH_PATH_NODE_BEGIN(path)                          \
  {                                                         \
    int _wpn_i;                                             \
    tree_node_t* node;                                     \
    for (_wpn_i = 0; _wpn_i < MAX_TREE_DEPTH; _wpn_i++) {   \
      if (_wpn_i == path->depth - 1) {                      \
        node = &(path->nodes[_wpn_i]);                      \

#define WITH_PATH_NODE_END                                  \
      }                                                     \
    }                                                       \
  }                                                         \

void tree_init(tree_t* tree);
void tree_find_eq(tree_t* tree, tree_key_t key, tree_result_set_t* result_set);
void tree_find_lt(tree_t* tree, tree_key_t key, BOOL equal_to, tree_result_set_t* result_set);
void tree_find_gt(tree_t* tree, tree_key_t key, BOOL equal_to, tree_result_set_t* result_set);
void tree_find_range(tree_t* tree, tree_key_t low_key, BOOL low_equal_to,
    tree_key_t high_key, BOOL high_equal_to, tree_result_set_t* result_set);
BOOL tree_insert(tree_t* tree, tree_key_t key, tree_value_t value);
BOOL tree_update(tree_t* tree, tree_key_t key, tree_value_t old_value, tree_value_t new_value);
BOOL tree_insert_no_balance(tree_t* tree, tree_key_t key, tree_value_t value, uint32_t* path_depth, uint32_t* tree_path);
BOOL tree_update_no_balance(tree_t* tree, tree_key_t key, tree_value_t old_value, tree_value_t value, uint32_t* path_depth, uint32_t* tree_path);

BOOL tree_remove(tree_t* tree, tree_key_t key);
BOOL tree_remove_value(tree_t* tree, tree_key_t key, tree_value_t value);
void tree_balance(tree_t* tree, uint32_t path_depth, uint32_t tree_path);

#pragma pack(pop)

#endif /* AVL_TREE_H_ */
