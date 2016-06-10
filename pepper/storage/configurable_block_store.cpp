#include <storage/configurable_block_store.h>

ConfigurableBlockStore::ConfigurableBlockStore() {
#ifdef USE_KYOTO_BLOCK_STORE
  blockStore = new KyotoBlockStore();
#elif defined (USE_DB_BLOCK_STORE)
  blockStore = new DBBlockStore();
#else
  blockStore = new LevelDBBlockStore();
#endif
}

ConfigurableBlockStore::ConfigurableBlockStore(string db_file_name) {
#ifdef USE_KYOTO_BLOCK_STORE
  blockStore = new KyotoBlockStore(db_file_name);
#elif defined (USE_DB_BLOCK_STORE)
  blockStore = new DBBlockStore(db_file_name);
#else
  blockStore = new LevelDBBlockStore(db_file_name);
#endif
}

ConfigurableBlockStore::~ConfigurableBlockStore() {
  delete blockStore;
}

void ConfigurableBlockStore::Open(string db_file_name) {
  blockStore->Open(db_file_name);
}

void ConfigurableBlockStore::Close() {
  blockStore->Close();
}

bool ConfigurableBlockStore::get(const Key& key, Value& value) {
  return blockStore->get(key, value);
}

void ConfigurableBlockStore::put(const Key& key, const Value& value) {
  blockStore->put(key, value);
}

bool ConfigurableBlockStore::getAddr(uint32_t addr, Value& value) {
  return blockStore->getAddr(addr, value);
}

void ConfigurableBlockStore::putAddr(uint32_t addr, const Value& value) {
  blockStore->putAddr(addr, value);
}

void ConfigurableBlockStore::free(const Key& key) {
  blockStore->free(key);
}

