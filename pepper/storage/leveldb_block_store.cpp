#include <iostream>
#include <sstream>
#include <time.h>
#include <vector>

#include <common/measurement.h>
#include <storage/leveldb_block_store.h>

static std::string addrToStr(uint32_t addr) {
  std::ostringstream oss;
  oss << "RAM_" << addr;
  return oss.str();
}


LevelDBBlockStore::LevelDBBlockStore() {
  opened = false;
  std::ostringstream oss;
  oss << "temp_block_store_" << PAPI_get_real_nsec();
  Open(oss.str());

#ifdef ASYNC_BSTORE
  // start a thread to consume key-value pairs in the queue
  multi_thread_running = true;
  pthread_create(&write_thread, NULL, &writeThreadFunction, this);
#endif
}

void* LevelDBBlockStore::writeThreadFunction(void* store) {
  ((LevelDBBlockStore*)store)->writeKVPairsToDisk();
  return NULL;
}

LevelDBBlockStore::LevelDBBlockStore(std::string db_file_name) {
  opened = false;
  Open(db_file_name);

#ifdef ASYNC_BSTORE
  // start a thread to consume key-value pairs in the queue
  multi_thread_running = true;
  pthread_create(&write_thread, NULL, &writeThreadFunction, this);
#endif
}

LevelDBBlockStore::~LevelDBBlockStore() {
#ifdef ASYNC_BSTORE
  multi_thread_running = false;
  // empty the queue to write the remaining key-value pairs to disk.
  std::vector<std::pair<HashBlockStore::Key, HashBlockStore::Value> > remaining = kv_pairs_to_write.FlushQueue();
  for (uint32_t i = 0; i < remaining.size(); i++) {
    synchronousPut(remaining[i].first, remaining[i].second);
  }
#endif

  Close();
}

void LevelDBBlockStore::writeKVPairsToDisk() {
  while (multi_thread_running) {
    std::pair<HashBlockStore::Key, HashBlockStore::Value> kv_pair = kv_pairs_to_write.Pop();
    synchronousPut(kv_pair.first, kv_pair.second);
  }
}

void LevelDBBlockStore::Open(std::string db_file_name) {
  Close();
  leveldb::Options options;
  options.create_if_missing = true;
  leveldb::Status status = leveldb::DB::Open(options, db_file_name, &db);
  if (!status.ok()) {
    std::cerr << db_file_name << " block store open error: " << status.ToString() << std::endl;
    std::cerr << "(You may need to run bin/pepper_prover setup first)" << std::endl;
    exit(1);
  }
  opened = true;
}

void LevelDBBlockStore::Close() {
  if (opened) {
    delete db;
    opened = false;
  }
}

bool LevelDBBlockStore::get(const HashBlockStore::Key& key, HashBlockStore::Value& value) {
#ifdef ASYNC_BSTORE
  // wait for the queue to be flushed before any get request can be served.
  std::vector<std::pair<HashBlockStore::Key, HashBlockStore::Value> > remaining = kv_pairs_to_write.FlushQueue();
  for (uint32_t i = 0; i < remaining.size(); i++) {
    synchronousPut(remaining[i].first, remaining[i].second);
  }
#endif
  int size_byte = key.size() / 8;
  char* key_buf = new char[size_byte];
  BitsToByteArray(key_buf, size_byte, key);
  int found = getVal(leveldb::Slice(key_buf, size_byte), value);
  delete[] key_buf;
  return found;
}

void LevelDBBlockStore::put(const HashBlockStore::Key& key, const HashBlockStore::Value& value) {

#ifdef ASYNC_BSTORE
  // push the key-value pair to the queue
  kv_pairs_to_write.Push(std::pair<HashBlockStore::Key, HashBlockStore::Value>(key, value));
#else
  synchronousPut(key, value);
#endif

}

void LevelDBBlockStore::synchronousPut(const HashBlockStore::Key& key, const HashBlockStore::Value& value) {
  int size_byte = key.size() / 8;
  char* key_buf = new char[size_byte];
  BitsToByteArray(key_buf, size_byte, key);
  putVal(leveldb::Slice(key_buf, size_byte), value);
  delete[] key_buf;
}

bool LevelDBBlockStore::getAddr(uint32_t addr, HashBlockStore::Value& value) {
  return getVal(leveldb::Slice((char*)&addr, sizeof(addr)), value);
}

void LevelDBBlockStore::putAddr(uint32_t addr, const HashBlockStore::Value& value) {
  putVal(leveldb::Slice((char*)&addr, sizeof(addr)), value);
}

void LevelDBBlockStore::free(const HashBlockStore::Key& key) {
  int size_byte = key.size() / 8;
  char* key_buf = new char[size_byte];
  BitsToByteArray(key_buf, size_byte, key);
  db->Delete(leveldb::WriteOptions(), leveldb::Slice(key_buf, size_byte));
  delete[] key_buf;
}

bool LevelDBBlockStore::getVal(const leveldb::Slice key, HashBlockStore::Value& value) {
  std::string vs;
  leveldb::Status s = db->Get(leveldb::ReadOptions(), key, &vs);
  bool found = s.ok();
  if (found) {
    value.resize(vs.size() * 8);
    ByteArrayToBits(vs.data(), vs.size(), value);
  }
  return found;
}

void LevelDBBlockStore::putVal(const leveldb::Slice key, const HashBlockStore::Value& value) {
  int size_byte = value.size() / 8;
  char* value_buf = new char[size_byte];
  BitsToByteArray(value_buf, size_byte, value);
  leveldb::Slice vs(value_buf, size_byte);

  db->Put(leveldb::WriteOptions(), key, vs);
  delete[] value_buf;
}
