#include <stdint.h>
#include <db.h>
#include <binary_tree_int_hash_t.h>

struct In {hash_t root;};
struct Out {uint32_t rows; uint32_t value;};

/*
 * The tree is created exogenously. Hopefully, this should be able to read the
 * exogenous tree.
 * 
 */
int compute(struct In *input, struct Out *output) {
    int tempInt, tempRowID;
    int nextRowID, numberOfRows, rowOffset, i;
    hash_t tempHash;

    tree_t Age_index;
    tree_result_set_t result;
    uint32_t age;

    Age_index.root = input->root;

    /*tree_init(&Age_index);*/
    /*age = 15;*/
    /*hashput(&tempHash, &age);*/
    /*tree_insert(&Age_index, 15, tempHash);*/

    /*uint32_t age = 24;*/
    age = 24;

    tree_find_lt(&(Age_index), age, FALSE, &(result));
    output->rows = result.num_results;
    hashget(&(output->value), &(result.results[0].value));
    return 0;
}
