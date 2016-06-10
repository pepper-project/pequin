#ifndef _DB_UTIL_H
#define _DB_UTIL_H

#include <iostream>
#include <stdint.h>
#include <stdlib.h>
#include <string>
#include <common/utility.h>
#include <include/avl_tree.h>
#include <include/db.h>
#include <storage/configurable_block_store.h>
#include <storage/exo.h>
#include <storage/ram_impl.h>

#pragma pack(push)
#pragma pack(1)

#include <apps/student_db.h>

int hash_t_comparator(const void* a, const void* b);

int int_comparator(const void* a, const void* b);

void print_hash(hash_t* hash);
void generate_random_permutation(tree_key_t* keys, int number_of_entries);

void setup(const char* folder_state, const char* bstore_file_name);
void generate_random_permutation_to_file(int number_of_rows, const char* filename, const char* folder_name);
void generate_one_chunk_of_db_row(long offset, int chunk_id, int number_of_entries, long entry_size, const char* folder_name, const char* folder_state);
int partition_tree_to_file(const char* tree_name, const char* folder_name, int tree_size, int max_depth);
void build_one_subtree_bottom_up(const char* tree_name, int subtree_id, const char* kv_pairs_filename, const char* folder_name);
void merge_trees(const char* tree_name, const char* kv_pairs_filename, const char* folder_name, int number_of_subtrees);

int build_tree_bottom_up(tree_t* tree, const char* tree_name, const char* filename, const char* folder_name, int size, int max_depth);

void merge_into_one_db(const char* folder_state, int number_of_chunks, const char* bstore_file_name);
void test_trees(Student_handle_t handle, int number_of_rows, const char* folder_state, const char* bstore_file_name);

Student_handle_t create_db(int number_of_rows, const char* bstore_file_name, const char* folder_state = NULL);
Student_handle_t create_db(int number_of_rows, int number_of_chunks, int max_depth, const char* bstore_file_name, const char* folder_state = NULL);
Student_handle_t create_compressed_db(int number_of_rows, const char* bstore_file_name, const char* folder_state = NULL);
Student_handle_t create_compressed_db(int number_of_rows, int number_of_chunks, int max_depth, const char* bstore_file_name, const char* folder_state = NULL);


#pragma pack(pop)
#endif /* db_util.h */
