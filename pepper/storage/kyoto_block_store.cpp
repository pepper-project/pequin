#include <sstream>
#include <iostream>

#include <common/measurement.h>
#include <storage/kyoto_block_store.h>

KyotoBlockStore::KyotoBlockStore() {
  opened = false;
  std::ostringstream oss;
  oss << "temp_block_store_" << PAPI_get_real_nsec();
  Open(oss.str());
}

KyotoBlockStore::KyotoBlockStore(std::string db_file_name) {
  opened = false;
  Open(db_file_name);
}

KyotoBlockStore::~KyotoBlockStore() {
  Close();
}

void KyotoBlockStore::Open(std::string db_file_name) {
  Close();
  if (!_blocks.open(db_file_name, kyotocabinet::DirDB::OWRITER | kyotocabinet::DirDB::OCREATE)) {
    std::cerr << db_file_name << "block store open error: " << _blocks.error().name() << std::endl;
    exit(1);
  }
  opened = true;
}

void KyotoBlockStore::Close() {
  if (opened) {
    _blocks.close();
    opened = false;
  }
}

static std::string keyToStr(const HashBlockStore::Key& k) {
  std::string ks;
  boost::to_string(k, ks);
  return ks;
}

bool KyotoBlockStore::get(const HashBlockStore::Key& k, HashBlockStore::Value& v) {
  return getVal(keyToStr(k), v);
}

void KyotoBlockStore::put(const HashBlockStore::Key& k, const HashBlockStore::Value& v) {
  putVal(keyToStr(k), v);
}

static std::string addrToStr(uint32_t addr) {
  std::ostringstream oss;
  oss << "RAM_" << addr;
  return oss.str();
}

bool KyotoBlockStore::getAddr(uint32_t addr, HashBlockStore::Value& v) {
  return getVal(addrToStr(addr), v);
}

void KyotoBlockStore::putAddr(uint32_t addr, const HashBlockStore::Value& v) {
  putVal(addrToStr(addr), v);
}

bool KyotoBlockStore::getVal(std::string key, Value& v) {
  if (!opened) {
    std::cout << "WARNING: trying to use an unopened block stored." << std::endl;
    return false;
  }
  std::string vs;
  bool found = _blocks.get(key, &vs);
  if (found) {
    v = Bits(vs);
  }

  return found;
}

void KyotoBlockStore::putVal(std::string key, const Value& v) {
  if (!opened) {
    std::cout << "WARNING: trying to use an unopened block stored." << std::endl;
    return;
  }
  std::string vs;
  boost::to_string(v, vs);

  _blocks.set(key, vs);
}

void KyotoBlockStore::free(const HashBlockStore::Key& k) {
  if (!opened) {
    std::cout << "WARNING: trying to use an unopened block stored." << std::endl;
    return;
  }
  std::string ks;
  boost::to_string(k, ks);
  _blocks.remove(ks);
}
