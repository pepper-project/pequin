#include <cassert>
#include <leveldb/comparator.h>
#include <leveldb/db.h>
#include <leveldb/options.h>
#include <leveldb/write_batch.h>
#include <storage/db_block_store.h>
#include "db_util.h"
#include "external_sort.h"

#define MAX(x,y) (((x) > (y))?(x):(y))

#define BUFSIZE 524288

#define my_hashput(buf, offset, hash, p_data) __my_hashput(buf, offset, hash, p_data, sizeof(*p_data))

#define READONLY

typedef struct key_value_pair {
  tree_key_t key;
  tree_value_t value;
  uint32_t offset;
} key_value_pair_t;

typedef struct subtree_info {
  int left;
  int right;
  int height;
  hash_t root;
  uint32_t root_index;
} subtree_info_t;

void print_hash(hash_t* hash) {
  unsigned char* ptr = (unsigned char*)hash;
  for (unsigned int i = 0; i < sizeof(hash_t); i++) {
    printf("%02X", ptr[i]);
  }
  printf("\n");
}

void __my_hashput(char* buf, long& offset, hash_t* hash, void* data, long size_in_bytes) {
  __hashbits(hash, data, size_in_bytes);
  memcpy(buf + offset, hash, sizeof(hash_t));
  offset += sizeof(hash_t);
  memcpy(buf + offset, data, size_in_bytes);
  offset += size_in_bytes;
  if ((offset / (sizeof(hash_t) + size_in_bytes)) % 100000 == 0) {
    cout << "current offset " << offset << endl;
  }
}

leveldb::DB* open_leveldb(const char* db_file_path) {
  leveldb::DB* db;
  leveldb::Options options;
  options.create_if_missing = true;
  leveldb::Status status = leveldb::DB::Open(options, db_file_path, &db);
  if (!status.ok()) {
    cout << "Error opening leveldb at " << db_file_path << endl;
    exit(1);
  }
  return db;
}

// generate a random permutation of keys using Knuth Shuffling algorithm.
void generate_random_permutation(tree_key_t* keys, int number_of_entries) {
  tree_key_t tmp;
  for (int i = 0; i < number_of_entries; i++) {
    keys[i] = i;
  }
  for (int i = number_of_entries - 1; i > 0; i--) {
    int j = rand() % (i + 1);
    tmp = keys[i];
    keys[i] = keys[j];
    keys[j] = tmp;
  }
}

void generate_random_permutation_to_file(int number_of_rows, const char* filename, const char* folder_name) {
  tree_key_t* keys = new tree_key_t[number_of_rows];
  generate_random_permutation(keys, number_of_rows);
  dump_array((char*)keys, number_of_rows * sizeof(tree_key_t), filename, folder_name);
  delete[] keys;
}

int hash_t_comparator(const void* a, const void* b) {
  const unsigned char* a_p = (const unsigned char*)a;
  const unsigned char* b_p = (const unsigned char*)b;
  uint32_t i = 0;
  while (i < sizeof(hash_t) - 1 && a_p[i] == b_p[i]) i++;
  return a_p[i] - b_p[i];
}

int int_comparator(const void* a, const void* b) {
  return *((int*)a) - *((int*)b);
}

// generate random permutation of the key and store the results in files.
void generate_random_permutation_as_keys(int number_of_rows) {
  cout << "permutating" << endl;
      generate_random_permutation_to_file(number_of_rows, "average_keys", FOLDER_TMP);
      generate_random_permutation_to_file(number_of_rows, "class_keys", FOLDER_TMP);
      generate_random_permutation_to_file(number_of_rows, "age_keys", FOLDER_TMP);
}

void generate_one_chunk_of_db_row(long offset, int chunk_id, int number_of_entries, long entry_size, const char* folder_name, const char* folder_state) {
  tree_key_t* average_buf = new tree_key_t[number_of_entries];
  tree_key_t* class_buf = new tree_key_t[number_of_entries];
  tree_key_t* age_buf = new tree_key_t[number_of_entries];

  key_value_pair_t* key_kv_pairs_buf = new key_value_pair_t[number_of_entries];
  key_value_pair_t* average_kv_pairs_buf = new key_value_pair_t[number_of_entries];
  key_value_pair_t* class_kv_pairs_buf = new key_value_pair_t[number_of_entries];
  key_value_pair_t* age_kv_pairs_buf = new key_value_pair_t[number_of_entries];
  char* hash_to_rows_buf = new char[number_of_entries * entry_size];

  Student_t* row_buf = new Student_t[number_of_entries];

  long buf_offset = 0;

  // read each chunk from disk
  // This should be a critical section
  load_array((char*)average_buf, number_of_entries * sizeof(tree_key_t), offset * sizeof(tree_key_t), "average_keys", folder_name);
  load_array((char*)class_buf, number_of_entries * sizeof(tree_key_t), offset * sizeof(tree_key_t), "class_keys", folder_name);
  load_array((char*)age_buf, number_of_entries * sizeof(tree_key_t), offset * sizeof(tree_key_t), "age_keys", folder_name);


  for (int k = 0; k < number_of_entries; k++) {
    Student_t tempStudent;
    hash_t tempHash;

    tempStudent.KEY = offset + k;
    tempStudent.FName = (uint64_t)rand() << 32 | (uint64_t)rand();
    tempStudent.LName = (uint64_t)rand() << 32 | (uint64_t)rand();
    tempStudent.Major = 10 + (rand() % 30);
    tempStudent.State = rand() % 50;
    tempStudent.PhoneNum = 512000000 + rand();
    tempStudent.Credits = rand();
    tempStudent.Honored = 0;

    for (uint32_t i = 0; i < sizeof(tempStudent.Address) / sizeof(uint64_t); i++) {
      tempStudent.Address.address[i] = (uint64_t)rand() << 32 | (uint64_t) rand();
    }

    tempStudent.Average = average_buf[k];
    tempStudent.Class = class_buf[k];
    tempStudent.Age = age_buf[k];

    my_hashput(hash_to_rows_buf, buf_offset, &tempHash, &tempStudent);

    key_kv_pairs_buf[k].key = tempStudent.KEY;
    average_kv_pairs_buf[k].key = tempStudent.Average;
    class_kv_pairs_buf[k].key = tempStudent.Class;
    age_kv_pairs_buf[k].key = tempStudent.Age;

    key_kv_pairs_buf[k].offset = average_kv_pairs_buf[k].offset = class_kv_pairs_buf[k].offset = age_kv_pairs_buf[k].offset = offset + k;
    key_kv_pairs_buf[k].value = average_kv_pairs_buf[k].value = class_kv_pairs_buf[k].value = age_kv_pairs_buf[k].value = tempHash;
    row_buf[k] = tempStudent;
  }

  // dump the chunk of key/value pairs onto disk.
  char filename[BUFLEN];

#ifndef READONLY
  snprintf(filename, BUFLEN - 1, "%s_%d", "key_kv_pairs", chunk_id);
  dump_array((char*)key_kv_pairs_buf, number_of_entries * sizeof(key_value_pair_t), filename, folder_name);
#else
  snprintf(filename, BUFLEN - 1, "%s_%d", "average_kv_pairs", chunk_id);
  dump_array((char*)average_kv_pairs_buf, number_of_entries * sizeof(key_value_pair_t), filename, folder_name);
  snprintf(filename, BUFLEN - 1, "%s_%d", "class_kv_pairs", chunk_id);
  dump_array((char*)class_kv_pairs_buf, number_of_entries * sizeof(key_value_pair_t), filename, folder_name);
  snprintf(filename, BUFLEN - 1, "%s_%d", "age_kv_pairs", chunk_id);
  dump_array((char*)age_kv_pairs_buf, number_of_entries * sizeof(key_value_pair_t), filename, folder_name);
#endif

  dump_array((char*)row_buf, number_of_entries * sizeof(Student_t), offset * sizeof(Student_t), "/block_stores/rows.db", folder_state);
#ifndef USE_DB_BLOCK_STORE
  snprintf(filename, BUFLEN - 1, "%s_%d", "hash_to_rows", chunk_id);
  dump_array((char*)hash_to_rows_buf, number_of_entries * entry_size, filename, folder_name);
#endif

  delete[] average_buf;
  delete[] class_buf;
  delete[] age_buf;
  delete[] key_kv_pairs_buf;
  delete[] average_kv_pairs_buf;
  delete[] class_kv_pairs_buf;
  delete[] age_kv_pairs_buf;
  delete[] row_buf;
  delete[] hash_to_rows_buf;
}

// generate rows for the db and store key-value pairs (where the key is the
// indexed column's value and the value is the hash of the row)
int generate_db_rows(int number_of_rows, int number_of_chunks, int entries_per_chunk, const char* folder_state) {
  cout << "creating rows" << endl;

  long entry_size = sizeof(hash_t) + sizeof(Student_t);
  int finished_chunks = 0;

  for (int i = 0; i < number_of_rows; i += entries_per_chunk) {
      int chunk_id = i / entries_per_chunk;
      int actual_entries_per_chunk = entries_per_chunk;
      if (number_of_rows - i < entries_per_chunk) {
          actual_entries_per_chunk = number_of_rows - i;      
      }

      generate_one_chunk_of_db_row(i, chunk_id, actual_entries_per_chunk, entry_size, FOLDER_TMP, folder_state);
      finished_chunks++;
      printf("generating rows: finished chunks = %d progress = %0.1f%%\n", finished_chunks, 100.0 * finished_chunks/number_of_chunks);
  }

  return finished_chunks;
}

// sort each file containing key-value pairs
void sort_indices(int number_of_rows, int number_of_chunks, int entries_per_chunk) {
  cout << "sorting" << endl;
  long entry_size = sizeof(key_value_pair_t);
  // sort each chunk in memory, probably 1 million rows each and then merge from disk
//#pragma omp parallel sections
  {
#ifndef READONLY
//#pragma omp section
    {
      cout << "sorting key index" << endl;
      external_sort("key_kv_pairs", FOLDER_TMP, number_of_chunks, entry_size, int_comparator, false);
    }
#else
//#pragma omp section
    {
      cout << "sorting average index" << endl;
      external_sort("average_kv_pairs", FOLDER_TMP, number_of_chunks, entry_size, int_comparator, false);
    }
//#pragma omp section
    {
      cout << "sorting class index" << endl;
      external_sort("class_kv_pairs", FOLDER_TMP, number_of_chunks, entry_size, int_comparator, false);
    }
//#pragma omp section
    {
      cout << "sorting age index" << endl;
      external_sort("age_kv_pairs", FOLDER_TMP, number_of_chunks, entry_size, int_comparator, false);
    }
#endif
  }
}

// partition the tree into subtrees, store the offset/index of the nodes
// that will be the ancesters of the subtrees.
void partition_tree(int& i, subtree_info_t* subtrees, int left, int right, int depth, int max_depth) {
  if (left >= right || depth > max_depth) {
    subtrees[i].left = left;
    subtrees[i].right = right;
    i++;
    return;
  }
  int mid = left + (right - left) / 2;

  partition_tree(i, subtrees, left, mid - 1, depth + 1, max_depth);
  partition_tree(i, subtrees, mid + 1, right, depth + 1, max_depth);
}

hash_t build_subtree(char* buf, long& offset, key_value_pair_t* kv_pairs, int left, int right, int* height) {
  if (left > right) {
    *height = 0;
    return *NULL_HASH;
  }

  tree_node node;
  int mid = left + (right - left) / 2;
  int hl, hr;

  node.left = build_subtree(buf, offset, kv_pairs, left, mid - 1, &hl);
  node.right = build_subtree(buf, offset, kv_pairs, mid + 1, right, &hr);

  node.key = kv_pairs[mid].key;
  node.num_values = 1;
  node.values[0] = kv_pairs[mid].value;

#ifdef AVL_TREE_H_
  node.height = MAX(hl, hr) + 1;
  node.height_left = hl;
  node.height_right = hr;
  if (height != NULL) {
    *height = node.height;
  }
#endif

  hash_t hash;
  my_hashput(buf, offset, &hash, &node);
  return hash;
}

void merge_trees(const char* tree_name, const char* kv_pairs_filename, const char* folder_name, int number_of_subtrees) {
  subtree_info_t subtrees[number_of_subtrees];
  for (int i = 0; i < number_of_subtrees; i++) {
    char full_filename[BUFLEN];
    snprintf(full_filename, BUFLEN - 1, "%s_%d_info", tree_name, i);
    load_array((char*)&subtrees[i], sizeof(subtree_info_t), full_filename, folder_name);
  }

  ifstream input_fp;
  open_file_read(input_fp, kv_pairs_filename, folder_name);

  char output_filename[BUFLEN];
  snprintf(output_filename, BUFLEN - 1, "%s_%d", tree_name, number_of_subtrees);

  long offset = 0;
  long entry_size = sizeof(hash_t) + sizeof(tree_node_t);
  char* buf = new char[number_of_subtrees * entry_size];

  tree_node_t top_nodes[number_of_subtrees / 2];
  number_of_subtrees = number_of_subtrees / 2;
  while (number_of_subtrees > 0) {
    for (int i = 0; i < number_of_subtrees; i++) {
      int mid = subtrees[i * 2].right + 1;
      subtrees[i].left = subtrees[i * 2].left;
      subtrees[i].right = subtrees[i * 2 + 1].right;

      key_value_pair_t kv_pair;
      input_fp.seekg(mid * sizeof(key_value_pair_t));
      if (!input_fp.read((char*)&kv_pair, sizeof(key_value_pair_t))) {
        cout << "Error reading from " << kv_pairs_filename << endl;
        exit(1);
      }

      top_nodes[i].key = kv_pair.key;
      top_nodes[i].num_values = 1;
      top_nodes[i].values[0] = kv_pair.value;

#ifdef AVL_TREE_H_
      top_nodes[i].height_left = subtrees[i * 2].height;
      top_nodes[i].height_right = subtrees[i * 2 + 1].height;
      top_nodes[i].height = MAX(top_nodes[i].height_left, top_nodes[i].height_right) + 1;
      subtrees[i].height = top_nodes[i].height;
#endif

      top_nodes[i].left = subtrees[i * 2].root;
      top_nodes[i].right = subtrees[i * 2 + 1].root;
      my_hashput(buf, offset, &(subtrees[i].root), &(top_nodes[i]));
    }
    number_of_subtrees /= 2;
  }
  input_fp.close();

  dump_array(buf, offset, output_filename, folder_name);
  delete[] buf;
  //return top_node_hashes[0];
  snprintf(output_filename, BUFLEN - 1, "%s_handle", tree_name);
  dump_array((char*)&subtrees[0].root, sizeof(hash_t), output_filename, folder_name);
}

void build_one_subtree_bottom_up(const char* tree_name, int subtree_id, const char* kv_pairs_filename, const char* folder_name) {
  subtree_info_t subtree_info;
  char full_filename[BUFLEN];
  snprintf(full_filename, BUFLEN - 1, "%s_%d_info", tree_name, subtree_id);
  load_array((char*)&subtree_info, sizeof(subtree_info_t), full_filename, folder_name);

  long offset = subtree_info.left * sizeof(key_value_pair_t);
  int subtree_size = subtree_info.right - subtree_info.left + 1;
  long entry_size = sizeof(hash_t) + sizeof(tree_node_t);
  cout << "subtree size " << subtree_size << endl;
  cout << "buf size " << subtree_size * entry_size;
  // read part of a sorted array and create a subtree out of it.
  key_value_pair_t* kv_pairs = new key_value_pair_t[subtree_size];
  // the seek and read should be atomic because other current threads might
  // also want to seek and read the same file. Interleaving between seek
  // and read would result in reading wrong data
  load_array((char*)kv_pairs, subtree_size * sizeof(key_value_pair_t), offset, kv_pairs_filename, folder_name);

  long buf_offset = 0;
  char* buf = new char[subtree_size * entry_size];
  subtree_info.root = build_subtree(buf, buf_offset, kv_pairs, 0, subtree_size - 1, &(subtree_info.height));

  if (buf_offset != subtree_size * entry_size) {
      cout << "Something is wrong with the buffer size. expected size " << subtree_size * entry_size << " actual size" << buf_offset << endl;
      exit(1);
      
    snprintf(full_filename, BUFLEN - 1, "%s_%d", tree_name, subtree_id);
    dump_array((char*)buf, subtree_size * entry_size, full_filename, folder_name);
    snprintf(full_filename, BUFLEN - 1, "%s_%d_info", tree_name, subtree_id);
    dump_array((char*)&subtree_info, sizeof(subtree_info_t), full_filename, folder_name);
  }

  delete[] kv_pairs;
  delete[] buf;
}

int partition_tree_to_file(const char* tree_name, const char* folder_name, int tree_size, int max_depth) {
  int max_number_of_subtrees = (2 << max_depth);
  subtree_info_t subtrees[max_number_of_subtrees];
  int number_of_subtrees = 0;
  partition_tree(number_of_subtrees, subtrees, 0, tree_size - 1, 1, max_depth);
  for (int i = 0; i < number_of_subtrees; i++) {
    char filename[BUFLEN];
    snprintf(filename, BUFLEN - 1, "%s_%d_info", tree_name, i);
    dump_array((char*)&subtrees[i], sizeof(subtree_info_t), filename, folder_name);
  }
  return number_of_subtrees;
}

int build_tree_bottom_up(tree_t* tree, const char* tree_name, const char* kv_pairs_filename, const char* folder_name, int tree_size, int max_depth) {
  int max_number_of_subtrees = (2 << max_depth);

  int number_of_subtrees = partition_tree_to_file(tree_name, folder_name, tree_size, max_depth);
  // create sub trees;
  int finished_chunks = 0;
  for (int i = 0; i < number_of_subtrees; i++) {
      build_one_subtree_bottom_up(tree_name, i, kv_pairs_filename, folder_name);
      finished_chunks++;
      printf("building %s: finished subtrees = %d progress = %0.1f%%\n", tree_name, finished_chunks, 100.0 * finished_chunks/number_of_subtrees);
  }
  
  // merge sub trees into one
  merge_trees(tree_name, kv_pairs_filename, folder_name, number_of_subtrees);
  char handle_filename[BUFLEN];
  snprintf(handle_filename, BUFLEN - 1, "%s_handle", tree_name);
  load_array((char*)&(tree->root), sizeof(hash_t), handle_filename, folder_name);
  return number_of_subtrees + 1;
}

Student_handle_t build_index_trees(int number_of_rows, int max_depth) {

  cout << "building tree" << endl;

  tree_t KEY_index;
  tree_t Average_index;
  tree_t Class_index;
  tree_t Age_index;

  Student_handle_t handle;
  memset(&handle, 0, sizeof(Student_handle_t));

  long entry_size = sizeof(hash_t) + sizeof(tree_node_t);

  // Up to this point, the index is already prepared as a sorted array stored
  // in a giant file. We next will create a self-certifying binary search tree
  // out of it.

  // build trees from bottom up
//#pragma omp parallel sections
  {
#ifndef READONLY
//#progma omp section
    {
      int number_of_chunks = build_tree_bottom_up(&KEY_index, "key_tree", "key_kv_pairs", FOLDER_TMP, number_of_rows, max_depth);
      handle.KEY_index = KEY_index.root;
    }
#else
//#pragma omp section
    {
      int number_of_chunks = build_tree_bottom_up(&Average_index, "average_tree", "average_kv_pairs", FOLDER_TMP, number_of_rows, max_depth);
      handle.Average_index = Average_index.root;
    }
//#pragma omp section
    {
      int number_of_chunks = build_tree_bottom_up(&Class_index, "class_tree", "class_kv_pairs", FOLDER_TMP, number_of_rows, max_depth);
      handle.Class_index = Class_index.root;
    }
//#pragma omp section
    {
      int number_of_chunks = build_tree_bottom_up(&Age_index, "age_tree", "age_kv_pairs", FOLDER_TMP, number_of_rows, max_depth);
      handle.Age_index = Age_index.root;
    }
#endif
  }

  return handle;
}

typedef struct chunk {
  const char* filename;
  long entry_size;
  long filesize;
  long offset;
  long offset_in_chunk;
  char* buffer;
  char* current_pointer;
  long chunk_size;
} chunk_t;

void read_one_chunk(chunk_t* chunk) {
  long remaining_bytes = chunk->filesize - chunk->offset;
  if (remaining_bytes > BUFSIZE * chunk->entry_size) {
    remaining_bytes = BUFSIZE * chunk->entry_size;
  }
  if (remaining_bytes <= 0) {
    chunk->current_pointer = NULL;
    cout << "file " << chunk->filename << " reached the end." << endl;
  } else {
    load_array(chunk->buffer, remaining_bytes, chunk->offset, chunk->filename, FOLDER_TMP);
    chunk->current_pointer = chunk->buffer;
    chunk->offset += remaining_bytes;
    chunk->offset_in_chunk = 0;
    chunk->chunk_size = remaining_bytes;
  }
}

void write_one_batch(leveldb::WriteBatch* batch, leveldb::DB* db) {
  leveldb::WriteOptions write_options;
  write_options.sync = true;
  // check if the batch is succeeded
  int num_retries = 0;
  while (num_retries < 100) {
    leveldb::Status s = db->Write(write_options, batch);
    if (s.ok()) {
      break;
    } else {
      cout << "an error occurred, will retry this batch" << endl;
    }
    num_retries++;
  }
  if (num_retries >= 100) {
    cout << "failed on batch after 100 retries" << endl;
    exit(1);
  }
  batch->Clear();
}

void merge_into_one_db(const char* folder_state, int number_of_chunks, const char* bstore_file_name) {
  char db_file_path[BUFLEN];

  snprintf(db_file_path, BUFLEN - 1, "%s/block_stores/", folder_state);
  recursive_mkdir(db_file_path, S_IRWXU);

  snprintf(db_file_path, BUFLEN - 1, "%s/block_stores/%s", folder_state, bstore_file_name);
  leveldb::DB* db = open_leveldb(db_file_path);

  int entry_size = sizeof(hash_t) + sizeof(Student_t);
  external_sort("hash_to_rows", FOLDER_TMP, number_of_chunks, entry_size, hash_t_comparator, false);

  entry_size = sizeof(hash_t) + sizeof(tree_node_t);

#ifndef READONLY
  external_sort("key_tree", FOLDER_TMP, number_of_chunks + 1, entry_size, hash_t_comparator, false);

  int number_of_parts = 2;
  chunk_t chunks[number_of_parts];
  chunks[0].entry_size = sizeof(hash_t) + sizeof(Student_t);
  chunks[0].filename = "hash_to_rows";
  chunks[1].entry_size = sizeof(hash_t) + sizeof(tree_node_t);
  chunks[1].filename = "key_tree";
#else
  external_sort("average_tree", FOLDER_TMP, number_of_chunks + 1, entry_size, hash_t_comparator, false);
  external_sort("class_tree", FOLDER_TMP, number_of_chunks + 1, entry_size, hash_t_comparator, false);
  external_sort("age_tree", FOLDER_TMP, number_of_chunks + 1, entry_size, hash_t_comparator, false);

  int number_of_parts = 4;
  chunk_t chunks[number_of_parts];
  chunks[0].entry_size = sizeof(hash_t) + sizeof(Student_t);
  chunks[0].filename = "hash_to_rows";
  chunks[1].entry_size = sizeof(hash_t) + sizeof(tree_node_t);
  chunks[1].filename = "average_tree";
  chunks[2].entry_size = sizeof(hash_t) + sizeof(tree_node_t);
  chunks[2].filename = "class_tree";
  chunks[3].entry_size = sizeof(hash_t) + sizeof(tree_node_t);
  chunks[3].filename = "age_tree";
#endif

  for (int i = 0; i < number_of_parts; i++) {
    chunks[i].buffer = new char[BUFSIZE * chunks[i].entry_size];
    char filename[BUFLEN];
    snprintf(filename, BUFLEN - 1, "%s/%s", FOLDER_TMP, chunks[i].filename);
    chunks[i].filesize = get_file_size(filename);
    chunks[i].offset = 0;
  }
  for (int i = 0; i < number_of_parts; i++) {
    read_one_chunk(&chunks[i]);
  }

  char* smallest_entry;
  int smallest_entry_index = -1;

  leveldb::WriteBatch batch;
  long buf_len = BUFSIZE * (sizeof(hash_t) + sizeof(tree_node_t));
  long offset = 0;
  char* write_buf = new char[buf_len];

  hash_t previous_hash = *NULL_HASH;

  int number_of_batches = 0;
  long number_of_records = 0;
  while (true) {
    smallest_entry = NULL;
    smallest_entry_index = -1;
    for (int i = 0; i < number_of_parts; i++) {
      if (chunks[i].current_pointer != NULL) {
        if (smallest_entry_index == -1 || hash_t_comparator(chunks[i].current_pointer, smallest_entry) < 0) {
          smallest_entry = chunks[i].current_pointer;
          smallest_entry_index = i;
        }
      }
    }
    // this means all chunks are over.
    if (smallest_entry_index == -1) {
      cout << "all files reached their end. finishing up." << endl;
      for (int i = 0; i < number_of_parts; i++) {
        cout << chunks[i].filename << " is at offset " << chunks[i].offset << endl;
      }
      break;
    }
    if (hash_t_comparator(&previous_hash, (hash_t*)smallest_entry) > 0) {
      cout << "hey, dude, you're messed up, the key-value pairs is not sorted. " << chunks[smallest_entry_index].filename << endl;
      print_hash(&previous_hash);
      print_hash((hash_t*)smallest_entry);
      exit(1);
    }
    previous_hash = *((hash_t*)smallest_entry);

    chunk_t* current_chunk = chunks + smallest_entry_index;

    if (offset >= buf_len) {
      write_one_batch(&batch, db);
      number_of_batches++;
      offset = 0;
      cout << number_of_batches << " batches finished" << endl;
      cout << number_of_records << " records finished" << endl;
    }
    //cout << "smallest chunk is " << current_chunk->filename << endl;
    //cout << current_chunk->filename << " is at offset " << current_chunk->offset_in_chunk << endl;

    memcpy(write_buf + offset, smallest_entry, current_chunk->entry_size);
    leveldb::Slice key_slice(write_buf + offset, sizeof(hash_t));
    leveldb::Slice value_slice(write_buf + offset + sizeof(hash_t), current_chunk->entry_size - sizeof(hash_t));

    batch.Put(key_slice, value_slice);
    offset += sizeof(hash_t) + sizeof(tree_node_t);
    number_of_records++;

    // move index forward for the next record.
    current_chunk->current_pointer += current_chunk->entry_size;
    current_chunk->offset_in_chunk += current_chunk->entry_size;

    // prepare the input buffer.
    if (current_chunk->offset_in_chunk >= current_chunk->chunk_size) {
      read_one_chunk(current_chunk);
    }
  }

  db->Write(leveldb::WriteOptions(), &batch);
  number_of_batches++;
  cout << number_of_batches << " batches finished" << endl;
  cout << number_of_records << " records finished" << endl;

  // clean up
  for (int i = 0; i < number_of_parts; i++) {
    delete[] chunks[i].buffer;
  }

  delete[] write_buf;

  delete db;
}

int height(hash_t* hash) {
  if (hasheq(hash, NULL_HASH)) {
    return 0;
  }
  tree_node_t node;
  hashget(&node, hash);
  return MAX(height(&node.left), height(&node.right)) + 1;
}

int x;
void inorder_traverse(hash_t* hash) {
  Student_t tempStudent;
  if (!hasheq(hash, NULL_HASH)) {
    tree_node_t node;

    hashget(&node, hash);
    //cout << node.key << endl;

    Student_t student;
    hashget(&student, &node.values[0]);
    if(node.height != height(hash)) {
     cout << "wrong height" << endl;
     exit(1);
    }
    if(abs(node.height_left - node.height_right) >= 2) {
      cout << "not balanced." << endl;
      exit(1);
    }

    inorder_traverse(&(node.left));
    if (x + 1 != node.key) {
      cout << "not a searchable binary tree. " << x + 1 << " " << node.key << endl;
      exit(1);
    }
    x = node.key;
    inorder_traverse(&(node.right));
  }
}

void test_tree(tree_t* tree, int number_of_rows) {
  x = -1;
  inorder_traverse(&(tree->root));
  if (x != number_of_rows - 1) {
    cout << "incomplete binary tree x = " << x << " expected = " << number_of_rows << endl;
    exit(1);
  }
}


void test_trees(Student_handle_t handle, int number_of_rows, const char* folder_state, const char* bstore_file_name) {
  cout << "testing tree" << endl;

  char db_file_path[BUFLEN];
  snprintf(db_file_path, BUFLEN - 1, "%s/block_stores", folder_state);
  recursive_mkdir(db_file_path, S_IRWXU);
  snprintf(db_file_path, BUFLEN - 1, "%s/block_stores/%s", folder_state, bstore_file_name);
  HashBlockStore* bs = new ConfigurableBlockStore(db_file_path);
  MerkleRAM* ram = new RAMImpl(bs);
  setBlockStoreAndRAM(bs, ram);

  tree_t KEY_index;
  tree_t Average_index;
  tree_t Class_index;
  tree_t Age_index;

#ifndef READONLY
  KEY_index.root = handle.KEY_index;
  cout << "testing key index" << endl;
  //test_tree(&KEY_index, number_of_rows);
#else
  Average_index.root = handle.Average_index;
  Class_index.root = handle.Class_index;
  Age_index.root = handle.Age_index;
  {
    cout << "testing average index" << endl;
    Student_t tempStudent;
    tree_result_set_t tempResult;
    tree_find_gt(&(Average_index), (90), TRUE, &(tempResult));
    for (int i = 0; i < tempResult.num_results; i++) {
      hashget(&(tempStudent), &(tempResult.results[i].value));
    }
    test_tree(&Average_index, number_of_rows);
  }
  {
    cout << "testing class index" << endl;
    Student_t tempStudent;
    tree_result_set_t tempResult;
    tree_find_eq(&(Class_index), (2009), &(tempResult));
    for (int i = 0; i < tempResult.num_results; i++) {
      hashget(&(tempStudent), &(tempResult.results[i].value));
    }
    test_tree(&Class_index, number_of_rows);
  }
  {
    cout << "testing age index" << endl;
    Student_t tempStudent;
    tree_result_set_t tempResult;
    tree_find_range(&(Age_index), (20), (FALSE), (24), (FALSE), &(tempResult));
    for (int i = 0; i < tempResult.num_results; i++) {
      hashget(&(tempStudent), &(tempResult.results[i].value));
    }
    test_tree(&Age_index, number_of_rows);
  }
#endif

  deleteBlockStoreAndRAM();

  cout << "all test passed" << endl;
}


void setup(const char* folder_state, const char* bstore_file_name) {
  assert(system("rm -rf " FOLDER_TMP) == 0);
  char buf[BUFLEN];
  snprintf(buf, BUFLEN - 1, "rm -rf %s/block_stores/*", folder_state);
  assert(system(buf) == 0);

  recursive_mkdir(FOLDER_TMP, S_IRWXU);
  recursive_mkdir(folder_state, S_IRWXU);

  snprintf(buf, BUFLEN - 1, "%s/block_stores", folder_state);
  recursive_mkdir(buf, S_IRWXU);

  srand(time(NULL));
}

void clean_up() {
}

Student_handle_t create_db(int number_of_rows, const char* bstore_file_name, const char* folder_state) {
  return create_db(number_of_rows, 16, 4, bstore_file_name, folder_state);
}

Student_handle_t create_db(int number_of_rows, int number_of_chunks, int max_depth, const char* bstore_file_name, const char* folder_state) {
  cout << "hash_t " << sizeof(hash_t) << endl;
  cout << "Student_t " << sizeof(Student_t) << endl;
  cout << "tree_node_t " << sizeof(tree_node_t) << endl;
  if (folder_state == NULL) {
    folder_state = FOLDER_STATE;
  }

  setup(folder_state, bstore_file_name);

  int entries_per_chunk = (number_of_rows + number_of_chunks - 1) / number_of_chunks;

  // generate random permutation.
  generate_random_permutation_as_keys(number_of_rows);

  // generate the rows of the DB.
  number_of_chunks = generate_db_rows(number_of_rows, number_of_chunks, entries_per_chunk, folder_state);

  // sort the index
  sort_indices(number_of_rows, number_of_chunks, entries_per_chunk);

  // build index trees.
  Student_handle_t handle = build_index_trees(number_of_rows, max_depth);

  // merge all sorted key-value pairs into a single levelDB.
  merge_into_one_db(folder_state, number_of_chunks, bstore_file_name);

//#ifdef DEBUG
  test_trees(handle, number_of_rows, folder_state, bstore_file_name);
//#endif

  clean_up();
  return handle;
}

void merge_compressed_trees(const char* tree_name, const char* kv_pairs_filename, const char* folder_name, const char* folder_state, int number_of_subtrees) {
  subtree_info_t subtrees[number_of_subtrees];
  tree_node_t top_nodes[number_of_subtrees];
  compressed_tree_node_t c_nodes[number_of_subtrees];

  for (int i = 0; i < number_of_subtrees; i++) {
    char full_filename[BUFLEN];
    snprintf(full_filename, BUFLEN - 1, "%s_%d_info", tree_name, i);
    load_array((char*)&subtrees[i], sizeof(subtree_info_t), full_filename, folder_name);
  }

  ifstream input_fp;
  open_file_read(input_fp, kv_pairs_filename, folder_name);

  char output_filename[BUFLEN];
  snprintf(output_filename, BUFLEN - 1, "%s_%d", tree_name, number_of_subtrees);

  long entry_size = sizeof(hash_t) + sizeof(tree_node_t);

  number_of_subtrees = number_of_subtrees / 2;
  while (number_of_subtrees > 0) {
    for (int i = 0; i < number_of_subtrees; i++) {
      int mid = subtrees[i * 2].right + 1;
      subtrees[i].left = subtrees[i * 2].left;
      subtrees[i].right = subtrees[i * 2 + 1].right;

      key_value_pair_t kv_pair;
      input_fp.seekg(mid * sizeof(key_value_pair_t));
      if (!input_fp.read((char*)&kv_pair, sizeof(key_value_pair_t))) {
        cout << "Error reading from " << kv_pairs_filename << endl;
        exit(1);
      }

      c_nodes[i].key = top_nodes[i].key = kv_pair.key;
      c_nodes[i].num_values = top_nodes[i].num_values = 1;
      top_nodes[i].values[0] = kv_pair.value;
      c_nodes[i].values[0] = kv_pair.offset;

#ifdef AVL_TREE_H_
      c_nodes[i].height_left = top_nodes[i].height_left = subtrees[i * 2].height;
      c_nodes[i].height_right = top_nodes[i].height_right = subtrees[i * 2 + 1].height;
      c_nodes[i].height = top_nodes[i].height = MAX(top_nodes[i].height_left, top_nodes[i].height_right) + 1;
      subtrees[i].height = top_nodes[i].height;
#endif

      top_nodes[i].left = subtrees[i * 2].root;
      top_nodes[i].right = subtrees[i * 2 + 1].root;
      c_nodes[i].left = subtrees[i * 2].root_index;
      c_nodes[i].right = subtrees[i * 2 + 1].root_index;

      // get hash to this node
      __hashbits(&(subtrees[i].root), &(top_nodes[i]), sizeof(tree_node_t));

      // write hash and compressed tree node
      subtrees[i].root_index = mid + 1;

      dump_array((char*)&c_nodes[i], sizeof(compressed_tree_node_t), subtrees[i].root_index * sizeof(compressed_tree_node_t), "/block_stores/index.db", folder_state);
      dump_array((char*)&subtrees[i].root, sizeof(hash_t), subtrees[i].root_index * sizeof(hash_t), "/block_stores/hash.db", folder_state);
    }
    number_of_subtrees /= 2;
  }
  input_fp.close();

  snprintf(output_filename, BUFLEN - 1, "%s_handle", tree_name);
  dump_array((char*)&subtrees[0].root, sizeof(hash_t), output_filename, folder_name);

  compressed_tree_node_t root;
  load_array((char*)&root, sizeof(compressed_tree_node_t), subtrees[0].root_index * sizeof(compressed_tree_node_t), "/block_stores/index.db", folder_state);
  snprintf(output_filename, BUFLEN - 1, "%s_root", tree_name);
  dump_array((char*)&root, sizeof(compressed_tree_node_t), output_filename, folder_name);
}

hash_t build_compressed_subtree(compressed_tree_node_t* c_node_buf, hash_t* hash_buf, long& offset, long& index_offset, key_value_pair_t* kv_pairs, int left, int right, int* height) {
  tree_node_t node;
  compressed_tree_node_t c_node;

  if (left > right) {
    *height = 0;
    return *NULL_HASH;
  }

  int mid = left + (right - left) / 2;
  int hl, hr;

  node.left = build_compressed_subtree(c_node_buf, hash_buf, offset, index_offset, kv_pairs, left, mid - 1, &hl);
  if (hl != 0) c_node.left = index_offset - 1; else c_node.left = 0;
  node.right = build_compressed_subtree(c_node_buf, hash_buf, offset, index_offset, kv_pairs, mid + 1, right, &hr);
  if (hr != 0) c_node.right = index_offset - 1; else c_node.right = 0;

  c_node.key = node.key = kv_pairs[mid].key;
  c_node.num_values = node.num_values = 1;
  node.values[0] = kv_pairs[mid].value;
  c_node.values[0] = kv_pairs[mid].offset;

#ifdef AVL_TREE_H_
  c_node.height = node.height = MAX(hl, hr) + 1;
  c_node.height_left = node.height_left = hl;
  c_node.height_right = node.height_right = hr;
  if (height != NULL) {
    *height = node.height;
  }
#endif

  hash_t hash;
  __hashbits(&hash, &node, sizeof(tree_node_t));
  // write the compressed tree node into file
  c_node_buf[offset] = c_node;
  hash_buf[offset] = hash;
  index_offset++;
  offset++;

  return hash;
}

void build_one_compressed_subtree_bottom_up(const char* tree_name, int subtree_id, long existing_index_entries, const char* kv_pairs_filename, const char* folder_name, const char* folder_state) {
  subtree_info_t subtree_info;
  char full_filename[BUFLEN];
  snprintf(full_filename, BUFLEN - 1, "%s_%d_info", tree_name, subtree_id);
  load_array((char*)&subtree_info, sizeof(subtree_info_t), full_filename, folder_name);

  long offset = 0;
  // 0 is reserved for NULL tree node.
  long index_offset = subtree_info.left + existing_index_entries;

  int subtree_size = subtree_info.right - subtree_info.left + 1;
  long entry_size = sizeof(hash_t) + sizeof(tree_node_t);
  //cout << "subtree size " << subtree_size << endl;

  compressed_tree_node_t* c_node_buf = new compressed_tree_node_t[subtree_size];
  hash_t* hash_buf = new hash_t[subtree_size];

  // read part of a sorted array and create a subtree out of it.
  key_value_pair_t* kv_pairs = new key_value_pair_t[subtree_size];
  // the seek and read should be atomic because other current threads might
  // also want to seek and read the same file. Interleaving between seek
  // and read would result in reading wrong data
  load_array((char*)kv_pairs, subtree_size * sizeof(key_value_pair_t), subtree_info.left * sizeof(key_value_pair_t), kv_pairs_filename, folder_name);
  subtree_info.root = build_compressed_subtree(c_node_buf, hash_buf, offset, index_offset, kv_pairs, 0, subtree_size - 1, &(subtree_info.height));
  subtree_info.root_index = index_offset - 1;

  snprintf(full_filename, BUFLEN - 1, "%s_%d_info", tree_name, subtree_id);
  dump_array((char*)&subtree_info, sizeof(subtree_info_t), full_filename, folder_name);

  dump_array((char*)c_node_buf, subtree_size * sizeof(compressed_tree_node_t), (subtree_info.left + existing_index_entries) * sizeof(compressed_tree_node_t), "/block_stores/index.db", folder_state);
  dump_array((char*)hash_buf, subtree_size * sizeof(hash_t), (subtree_info.left + existing_index_entries) * sizeof(hash_t), "/block_stores/hash.db", folder_state);
  

  delete[] kv_pairs;
  delete[] c_node_buf;
  delete[] hash_buf;
}

int build_compressed_tree_bottom_up(tree_t* tree, const char* tree_name, const char* kv_pairs_filename, const char* folder_name, const char* folder_state, long existing_index_entries, int tree_size, int max_depth) {
  int max_number_of_subtrees = (2 << max_depth);

  int number_of_subtrees = partition_tree_to_file(tree_name, folder_name, tree_size, max_depth);
  // create sub trees;
  int finished_chunks = 0;


  for (int i = 0; i < number_of_subtrees; i++) {
      build_one_compressed_subtree_bottom_up(tree_name, i, existing_index_entries, kv_pairs_filename, folder_name, folder_state);
      finished_chunks++;
      printf("building %s: finished subtrees = %d progress = %0.1f%%\n", tree_name, finished_chunks, 100.0 * finished_chunks/number_of_subtrees);
  }


  // merge sub trees into one
  merge_compressed_trees(tree_name, kv_pairs_filename, folder_name, folder_state, number_of_subtrees);
  char handle_filename[BUFLEN];
  snprintf(handle_filename, BUFLEN - 1, "%s_handle", tree_name);
  load_array((char*)&(tree->root), sizeof(hash_t), handle_filename, folder_name);
  return number_of_subtrees + 1;
}

void write_bootstrap_info_into_leveldb(const char* tree_name, leveldb::DB* db) {
  char filename[BUFLEN];
  hash_t root_hash;
  compressed_tree_node_t c_node;

  snprintf(filename, BUFLEN - 1, "%s_handle", tree_name);
  load_array((char*)&root_hash, sizeof(hash_t), filename, FOLDER_TMP);
  snprintf(filename, BUFLEN - 1, "%s_root", tree_name);
  load_array((char*)&c_node, sizeof(compressed_tree_node_t), filename, FOLDER_TMP);

  leveldb::Slice key_slice((char*)&root_hash, sizeof(hash_t));
  leveldb::Slice value_slice((char*)&c_node, sizeof(compressed_tree_node_t));

  db->Put(leveldb::WriteOptions(), key_slice, value_slice);
}

Student_handle_t build_compressed_index_trees(int number_of_rows, int max_depth, const char* folder_state, const char* bstore_file_name) {
  // now we have key to hash mapping for all indices
  // we need to create a compressed binary tree out of these sorted indices

  cout << "building tree" << endl;

  tree_t KEY_index;
  tree_t Average_index;
  tree_t Class_index;
  tree_t Age_index;

  long entry_size = sizeof(hash_t) + sizeof(tree_node_t);

  Student_handle_t handle;
  memset(&handle, 0, sizeof(Student_handle_t));

  // write bootstrap information into leveldb
  char db_file_path[BUFLEN];

  snprintf(db_file_path, BUFLEN - 1, "%s/block_stores/", folder_state);
  recursive_mkdir(db_file_path, S_IRWXU);

  snprintf(db_file_path, BUFLEN - 1, "%s/block_stores/%s", folder_state, bstore_file_name);
  leveldb::DB* db = open_leveldb(db_file_path);

  compressed_tree_node_t c_node;
  memset(&c_node, 0, sizeof(compressed_tree_node_t));
  dump_array((char*)&c_node, sizeof(compressed_tree_node_t), 0, "/block_stores/index.db", folder_state);
  dump_array((char*)NULL_HASH, sizeof(hash_t), 0, "/block_stores/hash.db", folder_state);

  long index_file_offset = 1;

  // Up to this point, the index is already prepared as a sorted array stored
  // in a giant file. We next will create a compressed self-certifying binary search tree
  // out of it.
#ifndef READONLY
  build_compressed_tree_bottom_up(&KEY_index, "key_tree", "key_kv_pairs", FOLDER_TMP, folder_state, index_file_offset, number_of_rows, max_depth);
  handle.KEY_index = KEY_index.root;
  write_bootstrap_info_into_leveldb("key_tree", db);
#else
  build_compressed_tree_bottom_up(&Average_index, "average_tree", "average_kv_pairs", FOLDER_TMP, folder_state, index_file_offset, number_of_rows, max_depth);
  index_file_offset += number_of_rows;
  build_compressed_tree_bottom_up(&Class_index, "class_tree", "class_kv_pairs", FOLDER_TMP, folder_state, index_file_offset, number_of_rows, max_depth);
  index_file_offset += number_of_rows;
  build_compressed_tree_bottom_up(&Age_index, "age_tree", "age_kv_pairs", FOLDER_TMP, folder_state, index_file_offset, number_of_rows, max_depth);

  handle.Average_index = Average_index.root;
  handle.Class_index = Class_index.root;
  handle.Age_index = Age_index.root;
  write_bootstrap_info_into_leveldb("average_tree", db);
  write_bootstrap_info_into_leveldb("class_tree", db);
  write_bootstrap_info_into_leveldb("age_tree", db);
#endif

  delete db;

  return handle;
}

void test_compressed_trees(Student_handle_t handle, int number_of_rows, const char* folder_state, const char* bstore_file_name) {
  cout << "testing tree" << endl;

  char db_file_path[BUFLEN];
  snprintf(db_file_path, BUFLEN - 1, "%s/block_stores", folder_state);
  recursive_mkdir(db_file_path, S_IRWXU);
  snprintf(db_file_path, BUFLEN - 1, "%s/block_stores/%s", folder_state, bstore_file_name);
  HashBlockStore* bs = new DBBlockStore(db_file_path);
  MerkleRAM* ram = new RAMImpl(bs);
  setBlockStoreAndRAM(bs, ram);

  tree_t KEY_index;
  tree_t Average_index;
  tree_t Class_index;
  tree_t Age_index;

#ifndef READONLY
  KEY_index.root = handle.KEY_index;
  cout << "testing key index" << endl;
  //test_tree(&KEY_index, number_of_rows);
#else
  Average_index.root = handle.Average_index;
  Class_index.root = handle.Class_index;
  Age_index.root = handle.Age_index;
  {
    cout << "testing average index" << endl;
    Student_t tempStudent;
    tree_result_set_t tempResult;
    tree_find_gt(&(Average_index), (90), TRUE, &(tempResult));
    for (int i = 0; i < tempResult.num_results; i++) {
      hashget(&(tempStudent), &(tempResult.results[i].value));
    }
    //test_tree(&Average_index, number_of_rows);
  }
  {
    cout << "testing class index" << endl;
    Student_t tempStudent;
    tree_result_set_t tempResult;
    tree_find_eq(&(Class_index), (2009), &(tempResult));
    for (int i = 0; i < tempResult.num_results; i++) {
      hashget(&(tempStudent), &(tempResult.results[i].value));
    }
    //test_tree(&Class_index, number_of_rows);
  }
  {
    cout << "testing age index" << endl;
    Student_t tempStudent;
    tree_result_set_t tempResult;
    tree_find_range(&(Age_index), (20), (FALSE), (24), (FALSE), &(tempResult));
    for (int i = 0; i < tempResult.num_results; i++) {
      hashget(&(tempStudent), &(tempResult.results[i].value));
    }
    //test_tree(&Age_index, number_of_rows);
  }
#endif

  deleteBlockStoreAndRAM();

  cout << "all test passed" << endl;
}

Student_handle_t create_compressed_db(int number_of_rows, const char* bstore_file_name, const char* folder_state) {
  return create_compressed_db(number_of_rows, 16, 4, bstore_file_name, folder_state);
}

Student_handle_t create_compressed_db(int number_of_rows, int number_of_chunks, int max_depth, const char* bstore_file_name, const char* folder_state) {
  cout << "sizeof(tree_key_t) " << sizeof(tree_key_t) << endl;
  cout << "sizeof(tree_value_t) " << sizeof(tree_value_t) << endl;
  cout << "sizeof(uint32_t) " << sizeof(uint32_t) << endl;
  cout << "sizeof(key_value_pair_t) " << sizeof(key_value_pair_t) << endl;

  if (folder_state == NULL) {
    folder_state = FOLDER_STATE;
  }

  setup(folder_state, bstore_file_name);

  int entries_per_chunk = (number_of_rows + number_of_chunks - 1) / number_of_chunks;

  // generate random permutation.
  generate_random_permutation_as_keys(number_of_rows);

  // generate the rows of the DB.
  number_of_chunks = generate_db_rows(number_of_rows, number_of_chunks, entries_per_chunk, folder_state);

  // sort the index
  sort_indices(number_of_rows, number_of_chunks, entries_per_chunk);

  // create compressed index tree
  Student_handle_t handle = build_compressed_index_trees(number_of_rows, max_depth, folder_state, bstore_file_name);

  clean_up();

  test_compressed_trees(handle, number_of_rows, folder_state, bstore_file_name);

  return handle;
}
