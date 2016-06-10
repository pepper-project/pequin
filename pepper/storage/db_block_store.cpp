#include <iostream>
#include <sstream>
#include <time.h>
#include <vector>

#include <common/utility.h>
#include <common/measurement.h>
#include <storage/db_block_store.h>
#include <storage/exo.h>
#include <storage/db_util.h>

DBBlockStore::DBBlockStore():row_file(NULL), index_file(NULL), hash_index_file(NULL), store(NULL) {
  std::ostringstream oss;
  oss << "temp_block_store_" << PAPI_get_real_nsec();

  Open(oss.str());

  row_size = sizeof(Student_t);
  tree_node_size = sizeof(tree_node_t);
  compressed_tree_node_size = sizeof(compressed_tree_node_t);
}

DBBlockStore::DBBlockStore(std::string db_file_name):row_file(NULL), index_file(NULL), hash_index_file(NULL), store(NULL) {
  Open(db_file_name);

  row_size = sizeof(Student_t);
  tree_node_size = sizeof(tree_node_t);
  compressed_tree_node_size = sizeof(compressed_tree_node_t);
}

DBBlockStore::~DBBlockStore() {
  Close();
}

void DBBlockStore::Open(std::string db_file_name) {
  Close();
  store = new LevelDBBlockStore(db_file_name);
  // prepare files that stores compressed index trees
  char buf[BUFLEN];
  snprintf(buf, BUFLEN - 1, "%s/../hash.db", db_file_name.c_str());
  hash_index_file = fopen(buf, "r+");
  if (hash_index_file == NULL) {
    printf("cannot open hash index file %s.\n", buf);
    exit(1);
  }
  snprintf(buf, BUFLEN - 1, "%s/../index.db", db_file_name.c_str());
  index_file = fopen(buf, "r+");
  if (index_file == NULL) {
    printf("cannot open hash index file %s.\n", buf);
    exit(1);
  }
  snprintf(buf, BUFLEN - 1, "%s/../rows.db", db_file_name.c_str());
  row_file = fopen(buf, "r+");
  if (row_file == NULL) {
    printf("cannot open hash index file %s.\n", buf);
    exit(1);
  }
}

void DBBlockStore::Close() {
  if (store != NULL)
    delete store;
  if (hash_index_file != NULL)
    fclose(hash_index_file);
  if (index_file != NULL)
    fclose(index_file);
  if (row_file != NULL)
    fclose(row_file);
}

void DBBlockStore::compressTreeNode(compressed_tree_node_t& c_tree_node, tree_node_t& f_tree_node) {
  // first copy some simple fields
  c_tree_node.key = f_tree_node.key;
  c_tree_node.height = f_tree_node.height;
  c_tree_node.height_left = f_tree_node.height_left;
  c_tree_node.height_right = f_tree_node.height_right;
  c_tree_node.num_values = f_tree_node.num_values;

  // compress hash to rows
  // look up the in memory cache to figure out hash to offset mapping
  HashBlockStore::Key hash_key(sizeof(hash_t) * 8);
  for (int i = 0; i < f_tree_node.num_values; i++) {
    ByteArrayToBits(&f_tree_node.values[i], sizeof(hash_t), hash_key);
    std::map<HashBlockStore::Key, uint32_t>::iterator it = row_hash_cache.find(hash_key);
    if (it == row_hash_cache.end()) {
      cout << "Cannot find the cached mapping for row that is supposed to exist." << endl;
      exit(1);
    }
    c_tree_node.values[i] = it->second;
  }

  // compress hash to left/right child
  if (hasheq(&f_tree_node.left, NULL_HASH)) {
    c_tree_node.left = 0;
  } else {
    ByteArrayToBits(&f_tree_node.left, sizeof(hash_t), hash_key);
    std::map<HashBlockStore::Key, uint32_t>::iterator it = index_hash_cache.find(hash_key);
    if (it == index_hash_cache.end()) {
      cout << "Cannot find the cached mapping for left child that is supposed to exist." << endl;
      exit(1);
    }
    c_tree_node.left = it->second;
  }
  if (hasheq(&f_tree_node.right, NULL_HASH)) {
    c_tree_node.right = 0;
  } else {
    ByteArrayToBits(&f_tree_node.right, sizeof(hash_t), hash_key);
    std::map<HashBlockStore::Key, uint32_t>::iterator it = index_hash_cache.find(hash_key);
    if (it == index_hash_cache.end()) {
      cout << "Cannot find the cached mapping for right child that is supposed to exist." << endl;
      exit(1);
    }
    c_tree_node.right = it->second;
  }
}


void DBBlockStore::decompressTreeNode(tree_node_t& f_tree_node, compressed_tree_node_t& c_tree_node) {
  // first copy some simple fields.
  //cout << "compressed tree node: row: " << c_tree_node.values[0] << " left: " << c_tree_node.left << " right: " << c_tree_node.right << endl;
  f_tree_node.key = c_tree_node.key;
  f_tree_node.height = c_tree_node.height;
  f_tree_node.height_left = c_tree_node.height_left;
  f_tree_node.height_right = c_tree_node.height_right;
  f_tree_node.num_values = c_tree_node.num_values;

  // populate hash to rows
  HashBlockStore::Key hash_key(sizeof(hash_t) * 8);
  char* row_buf = new char[row_size];
  for (int i = 0; i < f_tree_node.num_values; i++) {
    // read the row
    fseek(row_file, c_tree_node.values[i] * row_size, SEEK_SET);
    if (row_size != fread(row_buf, 1, row_size, row_file)) {
      cout << "Error reading row file." << endl;
      exit(1);
    }
    // calculate the hash and populate f_tree_node
    __hashbits(&f_tree_node.values[i], row_buf, row_size);
    // populate mapping for future row access.
    ByteArrayToBits(&f_tree_node.values[i], sizeof(hash_t), hash_key);
    row_hash_cache.insert(std::pair<HashBlockStore::Key, uint32_t>(hash_key, c_tree_node.values[i]));
  }
  delete[] row_buf;

  // populate mapping for future child node access.
  fseek(hash_index_file, c_tree_node.left * sizeof(hash_t), SEEK_SET);
  int ret = fread(&f_tree_node.left, 1, sizeof(hash_t), hash_index_file);
  if (sizeof(hash_t) != ret) {
    cout << "Error reading left child from hash index file. " << ret << endl;
    exit(1);
  }
  ByteArrayToBits(&f_tree_node.left, sizeof(hash_t), hash_key);
  index_hash_cache.insert(std::pair<HashBlockStore::Key, uint32_t>(hash_key, c_tree_node.left));

  fseek(hash_index_file, c_tree_node.right * sizeof(hash_t), SEEK_SET);
  ret = fread(&f_tree_node.right, 1, sizeof(hash_t), hash_index_file);
  if (sizeof(hash_t) != ret) {
    cout << "Error reading right child from hash index file. " << ret << endl;
    exit(1);
  }
  ByteArrayToBits(&f_tree_node.right, sizeof(hash_t), hash_key);
  index_hash_cache.insert(std::pair<HashBlockStore::Key, uint32_t>(hash_key, c_tree_node.right));
}

bool DBBlockStore::get(const HashBlockStore::Key& key, HashBlockStore::Value& value) {
  // first try to find metadata in hash cache.
  std::map<HashBlockStore::Key, uint32_t>::iterator it = row_hash_cache.find(key);
  if (it != row_hash_cache.end()) {
    char* value_buf = new char[row_size];
    // if metadata is found, read the corresponding data in row_file or
    // index_file.
    uint32_t offset = it->second;
    // read the content at the offset.
    //cout << "offset: " <<  offset << endl;
    fseek(row_file, offset * row_size, SEEK_SET);
    if (row_size != fread(value_buf, 1, row_size, row_file)) {
      cout << "Error reading row file." << endl;
      exit(1);
    }
    value.resize(row_size * 8);
    ByteArrayToBits(value_buf, row_size, value);
    delete[] value_buf;
    return true;
  } else {
    std::map<HashBlockStore::Key, uint32_t>::iterator it = index_hash_cache.find(key);
    if (it != index_hash_cache.end()) {
      tree_node_t f_tree_node;
      compressed_tree_node_t c_tree_node;

      // read the content of compressed tree node at the offset.
      uint32_t offset = it->second;
      //cout << "offset: " <<  offset << endl;
      fseek(index_file, offset * compressed_tree_node_size, SEEK_SET);
      if (compressed_tree_node_size != fread(&c_tree_node, 1, compressed_tree_node_size, index_file)) {
        cout << "Error reading index file." << endl;
        exit(1);
      }

      // construct tree node on the fly.
      decompressTreeNode(f_tree_node, c_tree_node);

      // assert the reconstructed hash matches the hash;
#ifdef DEBUG
      hash_t actual_hash, expected_hash;
      __hashbits(&actual_hash, &f_tree_node, sizeof(tree_node_t));
      BitsToByteArray((char*)&expected_hash, sizeof(hash_t), key);
      //print_hash(&actual_hash);
      //print_hash(&expected_hash);
      assert(hasheq(&expected_hash, &actual_hash));
#endif

      // prepare value
      value.resize(tree_node_size * 8);
      ByteArrayToBits(&f_tree_node, tree_node_size, value);
      return true;
    }
  }
  // if metadata is not found, pass the get request to leveldb store.
  bool ret = store->get(key, value);

  // try to read bootstrap information from level db blocks.
  if (ret) {
    if (value.size() == compressed_tree_node_size * 8) {
      tree_node_t f_tree_node;
      compressed_tree_node_t c_tree_node;
      BitsToByteArray(&c_tree_node, compressed_tree_node_size, value);

      // decompress the tree node
      decompressTreeNode(f_tree_node, c_tree_node);

      value.resize(tree_node_size * 8);
      ByteArrayToBits(&f_tree_node, tree_node_size, value);
    }
  }

  return ret;
}

void DBBlockStore::put(const HashBlockStore::Key& key, const HashBlockStore::Value& value) {
  if (value.size() == row_size * 8) {
    fseek(row_file, 0L, SEEK_END);
    uint32_t current_offset = ftell(row_file) / row_size;
    // well, write the content in row_file
    char* row_buf = new char[row_size];
    BitsToByteArray(row_buf, row_size, value);
    if (row_size != fwrite(row_buf, 1, row_size, row_file)) {
      cout <<  "Error writing row file." << endl;
      exit(1);
    }
    // remember the mapping from hash to file offset in the cache
    row_hash_cache.insert(std::pair<HashBlockStore::Key, uint32_t>(key, current_offset));

    delete[] row_buf;
  } else if (value.size() == tree_node_size * 8) {
    // convert value bits to a tree node
    tree_node_t f_tree_node;
    compressed_tree_node_t c_tree_node;

    BitsToByteArray(&f_tree_node, tree_node_size, value);
    // compress the node
    compressTreeNode(c_tree_node, f_tree_node);

    fseek(index_file, 0L, SEEK_END);
    uint32_t current_offset = ftell(index_file) / compressed_tree_node_size;
    // write the compressed tree node to index_file
    if (compressed_tree_node_size != fwrite(&c_tree_node, 1, compressed_tree_node_size, index_file)) {
      cout << "Error writing index file." << endl;
      exit(1);
    }

    // convert the full hash to byte array
    hash_t node_hash;
    BitsToByteArray(&node_hash, sizeof(hash_t), key);
    // write the full hash to hash_index_file
    fseek(hash_index_file, 0L, SEEK_END);
    if (sizeof(hash_t) != fwrite(&node_hash, 1, sizeof(hash_t), hash_index_file)) {
      cout << "Error writing hash index file." << endl;
      exit(1);
    }

    // remember the mapping in the cache
    index_hash_cache.insert(std::pair<HashBlockStore::Key, uint32_t>(key, current_offset));
  } else {
    store->put(key, value);
  }
}

// for RAM abstraction, still use LevelDB block store.
bool DBBlockStore::getAddr(uint32_t addr, HashBlockStore::Value& value) {
  return store->getAddr(addr, value);
}

void DBBlockStore::putAddr(uint32_t addr, const HashBlockStore::Value& value) {
  store->putAddr(addr, value);
}

void DBBlockStore::free(const HashBlockStore::Key& key) {
  store->free(key);
}
