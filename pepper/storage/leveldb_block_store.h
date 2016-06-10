#ifndef LEVELDB_BLOCK_STORE_H_
#define LEVELDB_BLOCK_STORE_H_

#include <leveldb/db.h>
#include <pthread.h>
#include <string>
#include <utility>

#include <storage/hash_block_store.h>
#include <storage/synchronized_queue.h>

class LevelDBBlockStore : public HashBlockStore {
  public:
    LevelDBBlockStore();
    LevelDBBlockStore(std::string db_file_name);
    virtual ~LevelDBBlockStore();
    virtual void Open(std::string db_file_name);
    virtual void Close();

    virtual bool get(const Key& key, Value& value);
    virtual void put(const Key& key, const Value& value);

    virtual bool getAddr(uint32_t addr, Value& value);
    virtual void putAddr(uint32_t addr, const Value& value);

    virtual void free(const Key& key);

  private:
    pthread_t write_thread;
    bool multi_thread_running;
    SynchronizedQueue<std::pair<HashBlockStore::Key, HashBlockStore::Value> > kv_pairs_to_write;
    leveldb::DB* db;

    static void* writeThreadFunction(void* store);

    void writeKVPairsToDisk();
    void synchronousPut(const HashBlockStore::Key& key, const HashBlockStore::Value& value);
    bool getVal(const leveldb::Slice key, Value& value);
    void putVal(const leveldb::Slice key, const Value& value);
};

#endif /* LEVELDB_BLOCK_STORE_H_ */

