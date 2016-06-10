#ifndef DB_BLOCK_STORE_H_
#define DB_BLOCK_STORE_H_

#include <include/avl_tree.h>

#include <map>
#include <string>
#include <utility>
#include <storage/hash_block_store.h>
#include <storage/leveldb_block_store.h>

// a special block store that is optimized for storage efficiency.
class DBBlockStore: public HashBlockStore {
  public:
    DBBlockStore();
    DBBlockStore(std::string db_file_name);
    virtual ~DBBlockStore();
    virtual void Open(std::string db_file_name);
    virtual void Close();

    virtual bool get(const Key& key, Value& value);
    virtual void put(const Key& key, const Value& value);

    virtual bool getAddr(uint32_t addr, Value& value);
    virtual void putAddr(uint32_t addr, const Value& value);

    virtual void free(const Key& key);

  private:
    // for row_file, a single offset is enough to locate a row. Then the hash
    // of the row can be calculated based on the content of the row.
    // We can do this because we always read row index from tree node.
    // from offset to row content.
    FILE* row_file;

    // for index_file, we need to store an extra hash to index mapping so that
    // when calculating hash of a tree node, we don't need to recalculate the
    // whole tree.
    // from offset to compressed tree node
    FILE* index_file;

    // from offset to hash of tree node.
    FILE* hash_index_file;

    // this stores temporary mapping from hash to tree node offset in index_file
    // this mapping is populated when accessing a compressed tree node.
    // the offset needs to be multiplied by the size of each entity.
    // this map will only be populated when the mapping from offset to full
    // hash is previously accessed. This is the case for tree_find and read
    // rows. This is also the case for tree insertion.
    std::map<HashBlockStore::Key, uint32_t> row_hash_cache;
    std::map<HashBlockStore::Key, uint32_t> index_hash_cache;

    size_t row_size;
    size_t tree_node_size;
    size_t compressed_tree_node_size;

    // This stores all but tree node and Db row blocks.
    // and necessary mapping to bootstrap the compressed index tree.
    LevelDBBlockStore* store;

    void compressTreeNode(compressed_tree_node_t &c_tree_node, tree_node_t &f_tree_node);
    void decompressTreeNode(tree_node_t &f_tree_node, compressed_tree_node_t &c_tree_node);
};

#endif /* DB_BLOCK_STORE_H_ */
