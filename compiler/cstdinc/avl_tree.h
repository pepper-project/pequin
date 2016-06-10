#ifndef _AVL_TREE_H
#define _AVL_TREE_H

#include <stdint.h>
#include <db.h>

#define MAX_TREE_DEPTH 12
#define MAX_TREE_RESULTS 5
#define MAX_TREE_NODE_VALUES 1

#define TREE_PATH_LEFT -1
#define TREE_PATH_EQUALS 0
#define TREE_PATH_RIGHT 1

#define KEY_T int
#define VALUE_T hash_t

#define BOOL int
#define TRUE 1
#define FALSE 0
#define NULL_PTR NULL_HASH

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

int MAX(int a, int b) {
  int max = a;
  if (b > a)
    max = b;
  return max;
}

int _value_equals(tree_value_t* v1, tree_value_t* v2) {
  return hasheq(v1, v2);
}

/*
 *      x                y
 *     / \             /   \
 *     y A             z   x
 *    / \     =>      / \ / \
 *    z B             C D B A
 *   / \
 *   C D
 */
void _rotate_left_left(tree_node_t* root) {
  tree_node_t old_root = *root;
  hashget(root, &(root->left));

  old_root.left = root->right;
  old_root.height_left = root->height_right;
  old_root.height = MAX(old_root.height_left, old_root.height_right) + 1;

  hashput(&(root->right), &old_root);
  root->height_right = old_root.height;
  root->height = MAX(root->height_left, root->height_right) + 1;
}

/*
 *      x                y
 *     / \             /   \
 *     A y             x   z
 *      / \   =>      / \ / \
 *      B z           A B C D
 *       / \
 *       C D
 */
void _rotate_right_right(tree_node_t* root) {
  tree_node_t old_root = *root;
  hashget(root, &(root->right));

  old_root.right = root->left;
  old_root.height_right = root->height_left;
  old_root.height = MAX(old_root.height_left, old_root.height_right) + 1;

  hashput(&(root->left), &old_root);
  root->height_left = old_root.height;
  root->height = MAX(root->height_left, root->height_right) + 1;
}

/*
 *      x            x             z
 *     / \          / \          /   \
 *     y A          z A          y   x
 *    / \     =>   / \     =>   / \ / \
 *    B z          y D          B C D A
 *     / \        / \
 *     C D        B C
 */
void _rotate_left_right(tree_node_t* root) {
  //tree_node_t left;
  //hashget(&left, &(root->left));
  //_rotate_right_right(&left);
  //hashput(&(root->left), &left);
  //_rotate_left_left(root);

  tree_node_t old_root = *root;
  tree_node_t left, left_right;

  hashget(&left, &(root->left));
  hashget(&left_right, &(left.right));

  old_root.left = left_right.right;
  old_root.height_left = left_right.height_right;
  old_root.height = MAX(old_root.height_left, old_root.height_right) + 1;

  left.right = left_right.left;
  left.height_right = left_right.height_left;
  left.height = MAX(left.height_left, left.height_right) + 1;

  *root = left_right;
  root->height_left = left.height;
  root->height_right = old_root.height;
  root->height = MAX(root->height_left, root->height_right) + 1;

  hashput(&(root->left), &left);
  hashput(&(root->right), &old_root);
}

/*
 *      x            x             z
 *     / \          / \          /   \
 *     A y          A z          x   y
 *      / \   =>     / \   =>   / \ / \
 *      z B          C y        A C D B
 *     / \            / \
 *     C D            D B
 */
void _rotate_right_left(tree_node_t* root) {
  //tree_node_t right;
  //hashget(&right, &(root->right));
  //_rotate_left_left(&right);
  //hashput(&(root->right), &right);
  //_rotate_right_right(root);

  tree_node_t old_root = *root;
  tree_node_t right, right_left;

  hashget(&right, &(root->right));
  hashget(&right_left, &(right.left));

  old_root.right = right_left.left;
  old_root.height_right = right_left.height_left;
  old_root.height = MAX(old_root.height_right, old_root.height_left) + 1;

  right.left = right_left.right;
  right.height_left = right_left.height_right;
  right.height = MAX(right.height_right, right.height_left) + 1;

  *root = right_left;
  root->height_right = right.height;
  root->height_left = old_root.height;
  root->height = MAX(root->height_right, root->height_left) + 1;

  hashput(&(root->right), &right);
  hashput(&(root->left), &old_root);
}

int _copy_value(tree_node_t *node, tree_value_t value) {
  BOOL success = FALSE;
  BOOL found = FALSE;
  int i;

  for (i = 0; i < MAX_TREE_NODE_VALUES; i++) {
    if (i < node->num_values && _value_equals(&(node->values[i]), &(value))) {
      found = TRUE;
    } else if (!found && i == node->num_values) {
      node->values[i] = value;
      success = TRUE;
    }
  }
  if (success) {
    node->num_values++;
  }

  return success;
}

int _replace_value(tree_node_t* node, tree_value_t old_value, tree_value_t new_value) {
  BOOL inserted = FALSE;
  BOOL replaced = FALSE;
  BOOL success = FALSE;
  int i;

  for (i = 0; i < MAX_TREE_NODE_VALUES; i++) {
    if (i < node->num_values && _value_equals(&(node->values[i]), &(old_value))) {
      node->values[i] = new_value;
      replaced = TRUE;
      success = TRUE;
    } else if (!replaced && i == node->num_values) {
      node->values[i] = new_value;
      inserted = TRUE;
      success = TRUE;
    }
  }
  if (inserted) {
    node->num_values++;
  }

  return success;
}

void _copy_result(tree_result_set_t* result_set, tree_key_t key, tree_value_t value) {
  int i;
  for (i = 0; i < MAX_TREE_RESULTS; i++) {
    if (i == result_set->num_results) {
      result_set->results[i].key = key;
      result_set->results[i].value = value;
    }
  }
  if (result_set->num_results < MAX_TREE_RESULTS) {
    result_set->num_results++;
  }
}

void _copy_results(tree_result_set_t* result_set, tree_node_t* node) {
  int i;
  for (i = 0; i < MAX_TREE_NODE_VALUES; i++) {
    if (i < node->num_values) {
      _copy_result(result_set, node->key, node->values[i]);
    }
  }
}

// Find the node with matching key if it exists, storing the path along the way
// if a matching key is not found, the path will be some path from the root to
// a leaf node whose key is greater than the searching key or whose in-order
// successor's key is greater than the searching key.
BOOL _find_path(tree_t* tree, tree_key_t key, tree_path_t* path) {
  int i;
  hash_t node_hash = tree->root;
  BOOL found = FALSE;

  for (i = 0; i < MAX_TREE_DEPTH; i++) {
    if (!found && !hasheq(&node_hash, NULL_HASH)) {
      tree_node_t node;
      hashget(&node, &node_hash);

      path->depth = i + 1;
      path->nodes[i] = node;

      if (key == node.key) {
        found = TRUE;
        path->branches[i] = TREE_PATH_EQUALS;
      } else if (key < node.key) {
        node_hash = node.left;
        path->branches[i] = TREE_PATH_LEFT;
      } else {
        node_hash = node.right;
        path->branches[i] = TREE_PATH_RIGHT;
      }
    }
  }

  return found;
}

int _find_first(tree_t* tree, tree_path_t* path) {
  int i;
  hash_t node_hash = tree->root;
  BOOL found = FALSE;

  for (i = 0; i < MAX_TREE_DEPTH; i++) {
    if (!hasheq(&node_hash, NULL_HASH)) {
      tree_node_t node;
      hashget(&node, &node_hash);

      found = TRUE;

      path->depth = i + 1;
      path->nodes[i] = node;
      path->branches[i] = TREE_PATH_LEFT;

      node_hash = node.left;
    }
  }

  return found;
}

int _find_next_below(tree_path_t* path) {
  int i;
  hash_t node_hash = *NULL_HASH;
  BOOL found = FALSE;
  int old_index = path->depth - 1;

  for (i = 0; i < MAX_TREE_DEPTH; i++) {
    if (i == old_index) {
      node_hash = path->nodes[i].right;
      path->branches[i] = TREE_PATH_RIGHT;
    } else if (i > old_index && !hasheq(&node_hash, NULL_HASH)) {
      tree_node_t node;
      hashget(&node, &node_hash);

      found = TRUE;

      path->depth = i + 1;
      path->nodes[i] = node;
      path->branches[i] = TREE_PATH_LEFT;

      node_hash = node.left;
    }
  }

  return found;
}

int _find_next_above(tree_path_t* path) {
  int i;
  BOOL found = FALSE;

  for (i = MAX_TREE_DEPTH - 1; i >= 0; i--) {
    if (!found && i < path->depth - 1 && path->branches[i] == TREE_PATH_LEFT) {
      found = TRUE;
      path->depth = i + 1;
    }
  }

  return found;
}

int _find_next(tree_path_t* path) {
  int found = _find_next_below(path);
  if (!found) {
    found = _find_next_above(path);
  }

  return found;
}

// Recompute the hashes along the path back up to the root
//
// BUG: Doesn't free old unlinked nodes
void _recompute_path(tree_t* tree, tree_path_t* path) {
  int i;
  tree_node_t previous_node;
  hash_t node_hash = *NULL_HASH;

  for (i = MAX_TREE_DEPTH - 1; i >= 0; i--) {
    if (i < path->depth) {
      tree_node_t* node = &(path->nodes[i]);

      if (i < path->depth - 1) {
        if (path->branches[i] == TREE_PATH_LEFT) {
          node->left = node_hash;
          node->height_left = previous_node.height;
          node->height = MAX(node->height_left, node->height_right) + 1;
          if (node->height_left - node->height_right == 2) {
            //if (_height(previous_node.left) - _height(previous_node.right) == 1)
            if (previous_node.height_left - previous_node.height_right == 1)
              _rotate_left_left(node);
            else
              _rotate_left_right(node);
          }
        } else if (path->branches[i] == TREE_PATH_RIGHT) {
          node->right = node_hash;
          node->height_right = previous_node.height;
          node->height = MAX(node->height_left, node->height_right) + 1;
          if (node->height_left - node->height_right == -2) {
            //if (_height(previous_node.left) - _height(previous_node.right) == -1)
            if (previous_node.height_left - previous_node.height_right == -1)
              _rotate_right_right(node);
            else
              _rotate_right_left(node);
          }
        }
      }
      previous_node = *node;
      hashput(&node_hash, node);
    }
  }

  tree->root = node_hash;
}

void _recompute_path_no_balance(tree_t* tree, tree_path_t* path, uint32_t* path_depth, uint32_t* tree_path) {
  int i;
  tree_node_t previous_node;
  previous_node.height = 0;
  previous_node.height_left = 0;
  previous_node.height_right = 0;

  hash_t node_hash = *NULL_HASH;

  *tree_path = 0;

  for (i = MAX_TREE_DEPTH - 1; i >= 0; i--) {
    if (i < path->depth) {
      tree_node_t* node = &(path->nodes[i]);

      if (i < path->depth - 1) {
        // 0 means left and 1 means right
        if (path->branches[i] == TREE_PATH_LEFT) {
          node->left = node_hash;
          node->height_left = previous_node.height;
          node->height = MAX(node->height_left, node->height_right) + 1;
        } else if (path->branches[i] == TREE_PATH_RIGHT) {
          *tree_path = *tree_path | (1 << i);
          node->right = node_hash;
          node->height_right = previous_node.height;
          node->height = MAX(node->height_left, node->height_right) + 1;
        }
      }
      previous_node = *node;
      hashput(&node_hash, node);
    }
  }

  *path_depth = path->depth;
  tree->root = node_hash;
}

void tree_init(tree_t* tree) {
  tree->root = *NULL_PTR;
}

void tree_find_eq(tree_t* tree, tree_key_t key, tree_result_set_t* result_set) {
  int i;
  hash_t node_hash = tree->root;
  BOOL found = FALSE;

  result_set->num_results = 0;

  for (i = 0; i < MAX_TREE_DEPTH; i++) {
    if (!found && !hasheq(&node_hash, NULL_HASH)) {
      tree_node_t node;
      hashget(&node, &node_hash);

      if (key == node.key) {
        found = TRUE;
        _copy_results(result_set, &node);
      } else if (key < node.key) {
        node_hash = node.left;
      } else {
        node_hash = node.right;
      }
    }
  }
}

void tree_find_lt(tree_t* tree, tree_key_t key, BOOL equal_to, tree_result_set_t* result_set) {
  int i;
  tree_path_t path;
  BOOL found = FALSE;
  path.depth = 0;
  result_set->num_results = 0;

  found = _find_first(tree, &path);
  if (found) {
    WITH_PATH_NODE_BEGIN((&path))
      _copy_results(result_set, node);
    WITH_PATH_NODE_END

    for (i = 0; i < MAX_TREE_RESULTS; i++) {
      found = _find_next(&path);
      if (found) {
        WITH_PATH_NODE_BEGIN((&path))
          if (node->key < key || (equal_to && node->key == key)) {
            _copy_results(result_set, node);
          }
        WITH_PATH_NODE_END
      }
    }
  }
}

void tree_find_gt(tree_t* tree, tree_key_t key, BOOL equal_to, tree_result_set_t* result_set) {
  int i;
  tree_path_t path;
  BOOL found = FALSE;
  path.depth = 0;
  result_set->num_results = 0;

  found = _find_path(tree, key, &path);
  if (path.depth > 0) {
    WITH_PATH_NODE_BEGIN((&path))
      if ((found && equal_to) || node->key > key) {
        _copy_results(result_set, node);
      }
    WITH_PATH_NODE_END

    for (i = 0; i < MAX_TREE_RESULTS; i++) {
      found = _find_next(&path);
      if (found) {
        WITH_PATH_NODE_BEGIN((&path))
          _copy_results(result_set, node);
        WITH_PATH_NODE_END
      }
    }
  }
}

void tree_find_range(tree_t* tree, tree_key_t low_key, BOOL low_equal_to,
    tree_key_t high_key, BOOL high_equal_to, tree_result_set_t* result_set) {
  int i;
  tree_path_t path;
  BOOL found = FALSE;
  path.depth = 0;
  result_set->num_results = 0;

  found = _find_path(tree, low_key, &path);
  if (path.depth > 0) {
    WITH_PATH_NODE_BEGIN((&path))
      if ((found && low_equal_to) || (node->key > low_key && node->key < high_key) ||
        (high_equal_to && node->key == high_key)) {
         _copy_results(result_set, node);
      }
    WITH_PATH_NODE_END

    for (i = 0; i < MAX_TREE_RESULTS; i++) {
      found = _find_next(&path);
      if (found) {
        WITH_PATH_NODE_BEGIN((&path))
          if ((node->key < high_key) || (high_equal_to && node->key == high_key)) {
            _copy_results(result_set, node);
          }
        WITH_PATH_NODE_END
      }
    }
  }
}

BOOL tree_insert(tree_t* tree, tree_key_t key, tree_value_t value) {
  BOOL found = FALSE;
  tree_path_t path;
  int i;
  BOOL success = FALSE;

  path.depth = 0;
  found = _find_path(tree, key, &path);

  if (found) {
    WITH_PATH_NODE_BEGIN((&path))
      success = _copy_value(node, value);
    WITH_PATH_NODE_END
  } else if (!found && path.depth < MAX_TREE_DEPTH) {
    path.depth++;
    WITH_PATH_NODE_BEGIN((&path))
      node->left = *NULL_PTR;
      node->right = *NULL_PTR;
      node->key = key;
      node->num_values = 1;
      node->values[0] = value;
      node->height = 1;
      node->height_left = 0;
      node->height_right = 0;
      success = TRUE;
    WITH_PATH_NODE_END
  }

  if (success) {
    _recompute_path(tree, &path);
  }

  return success;
}

BOOL tree_update(tree_t* tree, tree_key_t key, tree_value_t old_value, tree_value_t new_value) {
  BOOL found = FALSE;
  tree_path_t path;

  int i;
  BOOL success = FALSE;

  path.depth = 0;
  found = _find_path(tree, key, &path);

  if (found) {
    WITH_PATH_NODE_BEGIN((&path))
      success = _replace_value(node, old_value, new_value);
      //success = _copy_value(node, new_value);
    WITH_PATH_NODE_END
  } else if (!found && path.depth < MAX_TREE_DEPTH) {
    path.depth++;

    WITH_PATH_NODE_BEGIN((&path))
      node->left = *NULL_HASH;
      node->right = *NULL_HASH;
      node->key = key;
      node->num_values = 1;
      node->values[0] = new_value;
      node->height = 1;
      node->height_left = 0;
      node->height_right = 0;
      success = TRUE;
    WITH_PATH_NODE_END
  }

  if (success) {
    _recompute_path(tree, &path);
  }

  return success;
}

BOOL tree_insert_no_balance(tree_t* tree, tree_key_t key, tree_value_t value, uint32_t* path_depth, uint32_t* tree_path) {
  BOOL found = FALSE;
  tree_path_t path;
  int i;
  BOOL success = FALSE;

  path.depth = 0;
  found = _find_path(tree, key, &path);

  if (found) {
    WITH_PATH_NODE_BEGIN((&path))
      success = _copy_value(node, value);
    WITH_PATH_NODE_END
  } else if (!found && path.depth < MAX_TREE_DEPTH) {
    path.depth++;
    WITH_PATH_NODE_BEGIN((&path))
      node->left = *NULL_PTR;
      node->right = *NULL_PTR;
      node->key = key;
      node->num_values = 1;
      node->values[0] = value;
      node->height = 1;
      node->height_left = 0;
      node->height_right = 0;
      success = TRUE;
    WITH_PATH_NODE_END
  }

  if (success) {
    _recompute_path_no_balance(tree, &path, path_depth, tree_path);
  }

  return success;
}

BOOL tree_update_no_balance(tree_t* tree, tree_key_t key, tree_value_t old_value, tree_value_t new_value, uint32_t* path_depth, uint32_t* tree_path) {
  BOOL found = FALSE;
  tree_path_t path;
  int i;
  BOOL success = FALSE;

  path.depth = 0;
  found = _find_path(tree, key, &path);

  if (found) {
    WITH_PATH_NODE_BEGIN((&path))
      success = _replace_value(node, old_value, new_value);
    WITH_PATH_NODE_END
  } else if (!found && path.depth < MAX_TREE_DEPTH) {
    path.depth++;

    WITH_PATH_NODE_BEGIN((&path))
      node->left = *NULL_HASH;
      node->right = *NULL_HASH;
      node->key = key;
      node->num_values = 1;
      node->values[0] = new_value;
      node->height = 1;
      node->height_left = 0;
      node->height_right = 0;
      success = TRUE;
    WITH_PATH_NODE_END
  }

  if (success) {
    _recompute_path_no_balance(tree, &path, path_depth, tree_path);
  }

  return success;
}

// BUG: For now, removing a key just erases the values. It doesn't actually
// remove the node itself. Lame...
BOOL tree_remove(tree_t* tree, tree_key_t key) {
  BOOL found = FALSE;
  tree_path_t path;
  path.depth = 0;

  found = _find_path(tree, key, &path);
  if (found) {
    WITH_PATH_NODE_BEGIN((&path))
      node->num_values = 0;
    WITH_PATH_NODE_END
    _recompute_path(tree, &path);
  }

  return found;
}

BOOL tree_remove_value(tree_t* tree, tree_key_t key, tree_value_t value) {
  BOOL found = FALSE;
  tree_path_t path;

  int i;
  BOOL removed = FALSE;

  path.depth = 0;

  found = _find_path(tree, key, &path);
  if (found) {
    WITH_PATH_NODE_BEGIN((&path))
      for (i = 0; i < MAX_TREE_NODE_VALUES; i++) {
        if (i < node->num_values) {
          if (_value_equals(&(node->values[i]), &(value))) {
            removed = TRUE;
          } else if (removed && i > 0){
            node->values[i - 1] = node->values[i];
          }
        }
      }

      if (removed) {
        node->num_values--;
      }
    WITH_PATH_NODE_END

    if (removed) {
      _recompute_path(tree, &path);
    }
  }

  return found;
}

void _reconstruct_path(tree_t* tree, int path_depth, int tree_path, tree_path_t* path) {
  int i;
  hash_t node_hash = tree->root;

  for (i = 0; i < MAX_TREE_DEPTH; i++) {
    if (i < path_depth) {
      // 0 means left and 1 means right
      tree_node_t node;
      hashget(&node, &node_hash);

      path->depth = i + 1;
      path->nodes[i] = node;

      if (i < path_depth - 1) {
        int branch = (tree_path >> i) & 1;
        if (branch == 0) {
          node_hash = node.left;
          path->branches[i] = TREE_PATH_LEFT;
        } else {
          node_hash = node.right;
          path->branches[i] = TREE_PATH_RIGHT;
        }
      }
    }
  }
}

void tree_balance(tree_t* tree, uint32_t path_depth, uint32_t tree_path) {
  int i;
  tree_path_t path;
  tree_node_t previous_node;

  hash_t node_hash = *NULL_HASH;

  _reconstruct_path(tree, path_depth, tree_path, &path);

  for (i = MAX_TREE_DEPTH - 1; i >= 0; i--) {
    if (i < path.depth) {
      tree_node_t* node = &(path.nodes[i]);

      if (i < path.depth - 1) {
        if (path.branches[i] == TREE_PATH_LEFT) {
          if (node->height_left - node->height_right == 2) {
            if (previous_node.height_left - previous_node.height_right == 1)
              _rotate_left_left(node);
            else
              _rotate_left_right(node);
          }
        } else if (path.branches[i] == TREE_PATH_RIGHT) {
          if (node->height_left - node->height_right == -2) {
            if (previous_node.height_left - previous_node.height_right == -1)
              _rotate_right_right(node);
            else
              _rotate_right_left(node);
          }
        }
      }
      previous_node = *node;
      hashput(&node_hash, node);
    }
  }

  tree->root = node_hash;
}
#endif
