#ifndef _BINARY_TREE_H
#define _BINARY_TREE_H

#include <stdint.h>
#include "db.h"

#define BOOL int

#define MAX_TREE_DEPTH 4
#define MAX_TREE_RESULTS 4
#define MAX_TREE_NODE_VALUES 4

#define TREE_PATH_LEFT -1
#define TREE_PATH_EQUALS 0
#define TREE_PATH_RIGHT 1

typedef uint32_t tree_key_t;
typedef uint32_t tree_value_t;

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
} tree_node_t;

typedef struct tree_path {
	uint8_t depth;
	tree_node_t nodes[MAX_TREE_DEPTH];
	int8_t branches[MAX_TREE_DEPTH];
} tree_path_t;

#define WITH_PATH_NODE_BEGIN(path) 			    				\
	{															\
		int _wpn_i; 								    		\
		tree_node_t* node;								    	\
		for (_wpn_i = 0; _wpn_i < MAX_TREE_DEPTH; _wpn_i++) {  	\
			if (_wpn_i == path->depth - 1) {             		\
				node = &(path->nodes[_wpn_i]);       			\

#define WITH_PATH_NODE_END \
			}			   \
		}				   \
	}					   \

void tree_init(tree_t* tree);
void tree_find_eq(tree_t* tree, tree_key_t key, tree_result_set_t* result_set);
void tree_find_lt(tree_t* tree, tree_key_t key, BOOL equal_to, tree_result_set_t* result_set);
void tree_find_gt(tree_t* tree, tree_key_t key, BOOL equal_to, tree_result_set_t* result_set);
void tree_find_range(tree_t* tree, tree_key_t low_key, BOOL low_equal_to,
    tree_key_t high_key, BOOL high_equal_to, tree_result_set_t* result_set);
int tree_insert(tree_t* tree, tree_key_t key, tree_value_t value);
int tree_remove(tree_t* tree, tree_key_t key);
int tree_remove_value(tree_t* tree, tree_key_t key, tree_value_t value);

#endif
