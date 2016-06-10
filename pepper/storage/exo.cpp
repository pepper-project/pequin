// TODO: make sure to check the return status of the block-store ops;
// fail if they don't return true 
#include <common/measurement.h>
#include <common/utility.h>
#include <storage/configurable_block_store.h>
#include <storage/exo.h>
#include <storage/hasher.h>
#include <storage/ram_impl.h>

#ifdef DB_HASH_FUNC_IS_NULL
#include <storage/null_hash.h>
#else
#include <storage/ggh_hash.h>
#endif

//We need the implementation of some database functions here
//The code in compiler/cstdinc is compiled by zcc as well.
#include "../compiler/cstdinc/db.h"

//Use the same library implementation of commitment generation that the
//apps use

namespace {
  HashBlockStore* _blockStore = NULL;
  MerkleRAM* _ram = NULL;
#ifdef DB_HASH_FUNC_IS_NULL
  Hasher*_hasher = new NullHash();
#else
  Hasher*_hasher = new GGHHash();
#endif
}

void initBlockStore() {
  ostringstream oss;
  oss << "temp_block_store_" << PAPI_get_real_nsec();
  _blockStore = new ConfigurableBlockStore(oss.str());
  _ram = new RAMImpl(_blockStore);
}

HashType* getRootHash() {
  return _ram->getRootHash();
}

void setBlockStoreAndRAM(HashBlockStore* bs, MerkleRAM* ram) {
  if (bs == NULL || ram == NULL) {
    std::cout << "WARNING: Trying to set default block store/RAM to be NULL" << std::endl;
  }
  _blockStore = bs;
  _ram = ram;
}

void deleteBlockStoreAndRAM() {
  delete _ram;
  delete _blockStore;
}


// write the data to the persistent store
// update the root hash
void __ramput(MerkleRAM* ram, uint32_t addr, void* data, uint32_t size) {
  //size should be in bytes.
  Bits bits(RAM_CELL_NUM_BITS);
  ByteArrayToBits(data, size, bits);
  ram->put(addr, bits);
}

// read the data from persistent store and pad with zero if necessary.
void __ramget(MerkleRAM* ram, void* var, uint32_t addr, uint32_t size) {
  //size should be in bytes.
  Bits bits = ram->get(addr);
  BitsToByteArray(var, size, bits);
}

// write the data to the persistent store
// update the root hash
void __ramput(uint32_t addr, void* data, uint32_t size) {
  __ramput(_ram, addr, data, size);
}

// read the data from persistent store and pad with zero if necessary.
void __ramget(void* var, uint32_t addr, uint32_t size) {
  __ramget(_ram, var, addr, size);
}

void __hashbits(hash_t *hash, void *data, uint32_t size) {
  // compute hash of data.
  Bits bits(size * 8);
  ByteArrayToBits(data, size, bits);
  Bits hashBits = _hasher->hash(bits);

  for (uint32_t i = 0; i < hashBits.size() / 8; i++) {
    uint8_t* chunk = &(((uint8_t*)hash)[i]);
    *chunk = 0;
    for (uint32_t j = 0; j < 8; j++) {
      *chunk |= (hashBits[DB_HASH_NUM_BITS - (i * 8 + j) - 1] << j);
    }
  }
}

/* this should take a sized struct, calculate Merkle Damgard hash with GGH. and
 * return the hash result
   calculate the hash
   store the data in the persistent store key-value store. */
void __hashput(hash_t* hash, void* data, uint32_t size) {
  __hashput(_blockStore, hash, data, size);
}

void __hashget(void* var, hash_t* hash, uint32_t size) {
  __hashget(_blockStore, var, hash, size);
}
void hashfree(hash_t* hash){
  hashfree(_blockStore, hash);
}

void __commitmentput(commitment_t* hash, void* data, uint32_t size) {
  __commitmentput(_blockStore, hash, data, size);
}

void __commitmentget(void* var, commitment_t* hash, uint32_t size) {
  __commitmentget(_blockStore, var, hash, size);
}

void __hashput(HashBlockStore *bs, hash_t* hash, void* data, uint32_t size) {
  int numHashBits = _hasher->getNumHashBits();
  HashBlockStore::Key key(numHashBits);
  HashBlockStore::Value block(size * 8);
  // compute hash of data.
  ByteArrayToBits(data, size, block);
  Bits hashBits = _hasher->hash(block);
  for(int i = 0; i < numHashBits; i++) {
    key[i] = hashBits[numHashBits - i - 1];
  }

  // convert hash to key.
  // this key should be exactly the same as how the backend handles the hash.
  BitsToByteArray(hash, numHashBits/8, key);

  //put the block in kv store.
  bs->put(key, block);
}

void __hashget(HashBlockStore *bs, void* var, hash_t* hash, uint32_t size) {
  int numHashBits = _hasher->getNumHashBits();
  HashBlockStore::Key key(numHashBits);
  HashBlockStore::Value block(size * 8);
  ByteArrayToBits(hash, numHashBits/8, key);

  if (bs->get(key, block) != true) {
    cout<<"hashget: requested block not found"<<endl;
    exit(1);
  }

#ifdef DEBUG
  // test that the content matches the hash. This ensures that the block store
  // is self certifying.
  Bits hashBits = _hasher->hash(block);

  for(int i = 0; i < numHashBits; i++) {
    if(key[i] != hashBits[numHashBits - i - 1]) {
      cout << "hashget: hash not match." << endl;
      cout << "expectedHash: " << endl;
      for(int j = 0; j < numHashBits; j++) cout << key[j];
      cout << endl;
      cout << "actualHash: " << endl;
      for(int j = 0; j < numHashBits; j++) cout << hashBits[numHashBits - j - 1];
      cout << endl;
      exit(1);
    }
  }
#endif

  BitsToByteArray(var, size, block);
}

void hashfree(HashBlockStore *bs, hash_t* hash) {
  int numHashBits = _hasher->getNumHashBits();
  HashBlockStore::Key key(numHashBits);
  ByteArrayToBits(hash, numHashBits/8, key);

  bs->free(key);
}

void __commitmentput(HashBlockStore *bs, commitment_t* hash, void* data, uint32_t size) {
  int numHashBits = NUM_COMMITMENT_BITS;
  HashBlockStore::Key key(numHashBits);
  HashBlockStore::Value block(size * 8 + NUM_COMMITMENT_BITS_PER_BLOCK);

  //Compute Key from data
  uint8_t secretkey [NUM_COMMITMENT_BITS_PER_BLOCK/8];

  //randomly fill secretkey
  for(int i = 0; i < NUM_COMMITMENT_BITS_PER_BLOCK/8; i++){
    secretkey[i] = (uint8_t)rand();
  }

  commitment_hash((uint8_t*)data, size*8, secretkey, hash);

  ByteArrayToBits(hash->bit, numHashBits / 8, key);

  //Write (secret || data) to block
  ByteArrayToBits(secretkey, NUM_COMMITMENT_BITS_PER_BLOCK/8, block, 0);
  ByteArrayToBits(data, size, block, NUM_COMMITMENT_BITS_PER_BLOCK);

  //put the block in kv store.
  bs->put(key, block);
  /*
  cout << "Wrote key: " << endl;
  for(int i = 0; i < NUM_COMMITMENT_BITS; i++){
    cout << key[i];
  }
  cout << endl;
  */
}

void __commitmentget(HashBlockStore *bs, void* var, commitment_t* hash, uint32_t size) {
  int numHashBits = NUM_COMMITMENT_BITS;
  HashBlockStore::Key key(numHashBits);
  HashBlockStore::Value block(size * 8 + NUM_COMMITMENT_BITS_PER_BLOCK);
  ByteArrayToBits(hash->bit, numHashBits/8, key);

  if (bs->get(key, block) != true) {
    cout<<"commitmentget: requested block not found"<<endl;
    exit(1);
  }

  BitsToByteArray(var, size, block, NUM_COMMITMENT_BITS_PER_BLOCK);
}
