#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define FALSE 0
#define TRUE 1
#define BOOL int
#define DB_SIZE_OFFSET  0
#define DB_NUM_OF_ROW_OFFSET 1
#define DB_DATA_OFFSET 2

#define MAX_TREE_RESULTS 3
#define MAX_BLOCK_SIZE 768 * 7

#define ramput(addr, p_data) __ramput(addr, p_data, sizeof(*p_data))
#define ramget(p_data, addr) __ramget(p_data, addr, sizeof(*p_data))

/*#define _strcpy(dst, src) \
    { \
        int tempI; \
        char tempCharArr[] = src; \
        for (tempI = 0; tempI < sizeof(src); tempI++) { \
            dst[tempI] = src[tempI]; \
        } \
    }
*/

typedef struct data {
    int data[MAX_BLOCK_SIZE];
} data_t;

typedef int tree_key_t;
typedef int tree_value_t;

typedef struct tree_node {
    tree_key_t key;
    tree_value_t value;
    int key_size, value_size;
    struct tree_node* left;
    struct tree_node* right;
} tree_node_t;

typedef struct tree {
    tree_node_t* root;
} tree_t;

typedef struct tree_result {
    tree_key_t key;
    tree_value_t value;
    int key_size, value_size;
} tree_result_t;

typedef struct tree_result_set {
    int num_results;
    tree_result_t results[MAX_TREE_RESULTS];
} tree_result_set_t;

data_t ram[1024];

void tree_traverse_(tree_node_t* t) {
    if (t == 0) {
        return;
    }
    tree_traverse_(t->left);
    printf("%d: %d\n", t->key, t->value);
    tree_traverse_(t->right);
}

void tree_init(tree_t* t) {
  t->root = 0;
}

void tree_insert_(tree_node_t** t, tree_key_t key, tree_value_t value) {
    if (*t == 0) {
        *t = (tree_node_t*) malloc(sizeof(tree_node_t));
        (*t)->key = key;
        (*t)->value = value;
        (*t)->left = 0;
        (*t)->right = 0;
    } else if ((*t)->key > key) {
        tree_insert_(&((*t)->left), key, value);
    } else {
        tree_insert_(&((*t)->right), key, value);
    }
}

void tree_delete_(tree_node_t** t, tree_key_t key) {
    if (*t == 0) {
        return;
    } else if ((*t)->key > key) {
        tree_delete_(&((*t)->left), key);
    } else if ((*t)->key < key) {
        tree_delete_(&((*t)->right), key);
    } else {
        if((*t)->left == 0 && (*t)->right == 0) {
            free(*t);
        } else if ((*t)->left == 0) {
            tree_node_t* temp = *t;
            *t = (*t)->right;
            free(*t);
        } else if ((*t)->right == 0) {
            tree_node_t* temp = *t;
            *t = (*t)->left;
            free(*t);
        } else {
            tree_node_t** min = &((*t)->right);
            while((*min)->left != 0) {
                min = &((*min)->left);
            }
            tree_node_t** temp = min;
            (*t)->key = (*temp)->key;
            (*t)->value = (*temp)->value;
            tree_node_t* temp1 = *temp;
            *temp = (*temp)->right;
            free(temp1);
        }
    }
}

int tree_find_EQ_(tree_node_t* t, tree_result_set_t* var, tree_key_t key, int i) {
    if (t == 0) {
        return 0;
    } else if (t->key > key) {
        return tree_find_EQ_(t->left, var, key, i);
    } else if (t->key < key) {
        return tree_find_EQ_(t->right, var, key, i);
    } else {
        i += tree_find_EQ_(t->left, var, key, i);
        if (i < MAX_TREE_RESULTS) {
            var->results[i].key = t->key;
            var->results[i].value = t->value;
            i++;
        }
        i += tree_find_EQ_(t->right, var, key, i);
        return i;
    }
}

int tree_find_LT_(tree_node_t* t, tree_result_set_t* var, tree_key_t key, BOOL equal_to, int i) {
    if (t == 0) {
        return i;
    } else if (t->key > key) {
        return tree_find_LT_(t->left, var, key, equal_to, i);
    } else {
        i = tree_find_LT_(t->left, var, key, equal_to, i);
        if(equal_to || t->key < key) {
            if (i < MAX_TREE_RESULTS) {
                var->results[i].key = t->key;
                var->results[i].value = t->value;
                i++;
            }
            i = tree_find_LT_(t->right, var, key, equal_to, i);
        }
        return i;
    }
}

int tree_find_GT_(tree_node_t* t, tree_result_set_t* var, tree_key_t key, BOOL equal_to, int i) {
    if (t == 0) {
        return i;
    } else if (t->key < key) {
        return tree_find_GT_(t->right, var, key, equal_to, i);
    } else {
        if(equal_to || t->key > key) {
            i = tree_find_GT_(t->left, var, key, equal_to, i);
            if (i < MAX_TREE_RESULTS) {
                var->results[i].key = t->key;
                var->results[i].value = t->value;
                i++;
            }
        }
        i = tree_find_GT_(t->right, var, key, equal_to, i);
        return i;
    }
}

void __ramput(int addr, void* value, int size) {
    memcpy(&(ram[addr]), value, size);
}

void __ramget(void* var, int addr, int size) {
    memcpy(var, &(ram[addr]), size);
}

void tree_traverse(tree_t* t) {
    tree_traverse_(t->root);
}

void tree_insert(tree_t* t, tree_key_t key, tree_value_t value) {
    tree_insert_(&(t->root), key, value);
}

void tree_remove(tree_t *t, tree_key_t key) {
    tree_delete_(&(t->root), key);
}

void tree_remove_value(tree_t *t, tree_key_t key, tree_value_t value) {
//    tree_delete_value_(&(t->root), key, value);
}

void tree_find_eq(tree_t* t, tree_key_t key, tree_result_set_t* var) {
    var->num_results = tree_find_EQ_(t->root, var, key, 0);
}

void tree_find_lt(tree_t* t, tree_key_t key, BOOL equal_to, tree_result_set_t* var) {
    var->num_results = tree_find_LT_(t->root, var, key, equal_to, 0);
}

void tree_find_gt(tree_t* t, tree_key_t key, BOOL equal_to, tree_result_set_t* var) {
    var->num_results = tree_find_GT_(t->root, var, key, equal_to, 0);
}
