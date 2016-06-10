#ifndef HASH_BLOCK_STORE_H_
#define HASH_BLOCK_STORE_H_

#include <stdint.h>
#include <storage/storage.h>
#include <string>

/*
 * This class is a wrapper of underlying key-value store to provide key-value
 * store abstraction.
 *
 */
class HashBlockStore {
public:
  typedef Bits Key;
  typedef Bits Value;

  HashBlockStore() {}
  virtual ~HashBlockStore() {}

  // we should try to guarantee that at any time there is only one instance of
  // each underlying block store.
  virtual void Open(std::string db_path_file) {}
  virtual void Close() {}

  bool isOpened() {return opened;}
  virtual bool get(const Key& k, Value& value) = 0;
  virtual void put(const Key& k, const Value& v) = 0;

  virtual bool getAddr(uint32_t addr, Value& v) = 0;
  virtual void putAddr(uint32_t addr, const Value& v) = 0;

  virtual void free(const Key& k) = 0;
protected:
  bool opened;
};

#endif /* HASH_BLOCK_STORE_H_ */
