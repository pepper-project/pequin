#ifndef BINARY_TREE_INT_HASH_T_H_
#define BINARY_TREE_INT_HASH_T_H_

#include "db.h"

#define MAX_TREE_DEPTH 11
#define MAX_TREE_RESULTS 5
#define MAX_TREE_NODE_VALUES 1

#define KEY_T int
#define VALUE_T hash_t

#pragma pack(push)
#pragma pack(1)

#include "binary_tree.h.tmpl"

#pragma pack(pop)

#endif //BINARY_TREE_INT_HASH_T_H_
