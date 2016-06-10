#ifndef CONFIGURABLE_BLOCK_STORE_H_
#define CONFIGURABLE_BLOCK_STORE_H_

#include <storage/hash_block_store.h>
#include <string>
#include <time.h>
#include <iostream>
#include <storage/kyoto_block_store.h>
#include <storage/leveldb_block_store.h>
#include <storage/db_block_store.h>
//#define USE_KYOTO_BLOCK_STORE
//#define USE_DB_BLOCK_STORE

using namespace std;

class ConfigurableBlockStore : public HashBlockStore {
  public:
    ConfigurableBlockStore();
    ConfigurableBlockStore(string db_file_name);
    virtual ~ConfigurableBlockStore();
    virtual void Open(string db_file_name);
    virtual void Close();

    virtual bool get(const Key& key, Value& value);
    virtual void put(const Key& key, const Value& value);

    virtual bool getAddr(uint32_t addr, Value& value);
    virtual void putAddr(uint32_t addr, const Value& value);

    virtual void free(const Key& key);

  private:
    HashBlockStore* blockStore;
};

#endif /* KYOTO_BLOCK_STORE_H_ */

