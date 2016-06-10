#ifndef KYOTO_BLOCK_STORE_H_
#define KYOTO_BLOCK_STORE_H_

#include <kcdirdb.h>
#include <string>
#include <time.h>

#include <storage/hash_block_store.h>

//using namespace std;

class KyotoBlockStore : public HashBlockStore {
  public:
    KyotoBlockStore();
    KyotoBlockStore(std::string db_file_name);
    virtual ~KyotoBlockStore();
    virtual void Open(std::string db_file_name);
    virtual void Close();

    virtual bool get(const Key& k, Value& value);
    virtual void put(const Key& k, const Value& v);

    virtual bool getAddr(uint32_t addr, Value& value);
    virtual void putAddr(uint32_t addr, const Value& v);

    virtual void free(const Key& k);

  private:
    kyotocabinet::DirDB _blocks;

    bool getVal(std::string key, Value& value);
    void putVal(std::string key, const Value& v);
};

#endif /* KYOTO_BLOCK_STORE_H_ */
