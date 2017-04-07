#include <string>
#include <iostream>
#include <sstream>
#include <cassert>

#include <storage/hash_block_store.h>
#include <storage/ram_impl.h>

#ifdef DB_HASH_FUNC_IS_NULL
#include <storage/null_hash.h>
#else
#include <storage/ggh_hash.h>
#endif



RAMImpl::RAMImpl(HashBlockStore* blockStore) :
  _blockStore(blockStore),
  _numLevels(0),
#ifdef DB_HASH_FUNC_IS_NULL
  _hasher(new NullHash()),
#else
  _hasher(new GGHHash()),
#endif
  _rootHash(NULL) {

#ifdef DB_HASH_FUNC_IS_NULL
    cout << "WARNING: Using null hash - not cryptographically secure!" << endl;
#endif

    Bits b(1);
    _emptyHash = _hasher->hash(b);

    if (!_hashCache.open("-", kyotocabinet::CacheDB::OWRITER | kyotocabinet::CacheDB::OCREATE)) {
      std::cerr << "RAM impl hash cache open error: " << _hashCache.error().name() << std::endl;
      exit(1);
    }

    if (DB_NUM_ADDRESSES > 0) {
      _numLevels = 1;
      while (DB_NUM_ADDRESSES > (1 << (_numLevels - 1))) _numLevels++;
    } else {
      _numLevels = 0;
    }

    updateHashes();
  }

RAMImpl::~RAMImpl() {
  if (_rootHash != NULL)
    delete _rootHash;
  delete _hasher;
}

static std::string uintToStr(uint32_t i) {
  std::ostringstream oss;
  oss << i;
  return oss.str();
}

Bits RAMImpl::get(uint32_t addr) {
  Bits bits;

  if (addr > DB_NUM_ADDRESSES) {
    std::cerr << "Address out of bound: " << addr << std::endl;
    exit(1);
  }
  bool found = _blockStore->getAddr(addr, bits);
  if (!found) {
    bits = Bits(RAM_CELL_NUM_BITS);
  }

  return bits;
}

void RAMImpl::put(uint32_t addr, const Bits& val) {
  _blockStore->putAddr(addr, val);
  invalidateHashes(addr);
  updateHashes();
}

HashType* RAMImpl::getRootHash() {
  return _rootHash;
}

int RAMImpl::getNumHashBits() {
  return _hasher->getNumHashBits();
}

size_t RAMImpl::getHCIndex(uint32_t addr, uint32_t level, bool shift) {
  assert(addr >= 0);

  if (shift) {
    addr >>= (_numLevels - level - 1);
  }

  return (1 << level) - 1 + addr;
}

bool RAMImpl::getSiblingHash(uint32_t addr, uint32_t level, Bits& hash) {
  assert(level > 0); // the root doesn't have a sibling

  int cacheIndex = getHCIndex(addr, level, true);
  cacheIndex = (cacheIndex & 1) ? cacheIndex + 1 : cacheIndex - 1;
  return getHash(cacheIndex, hash);

  // Eventually, we'll probably handle cache misses like this
  //	addr >>= (_numLevels - level - 1);
  //	addr = (addr & 1) ? addr + 1 : addr - 1;
  //	hash = updateSubtree(addr, level);
  //  	return true;
}

bool RAMImpl::getHash(uint32_t addr, uint32_t level, bool shift, Bits& hash) {
  return getHash(getHCIndex(addr, level, shift), hash);
}

bool RAMImpl::getHash(uint32_t cacheIndex, Bits& hash) {
  assert(cacheIndex >= 0);

  //  std::cerr << "getHash: cacheIndex=" << cacheIndex << std::endl;

  std::string hashStr;
  bool found = _hashCache.get(uintToStr(cacheIndex), &hashStr);
  if (found) {
    hash = Bits(hashStr);
  }

  return found;
}

void RAMImpl::putHash(uint32_t addr, uint32_t level, const Bits& hash, bool shift) {
  std::string hashStr;
  boost::to_string(hash, hashStr);
  _hashCache.set(uintToStr(getHCIndex(addr, level, shift)), hashStr);
}

void RAMImpl::delHash(uint32_t addr, uint32_t level, bool shift) {
  _hashCache.remove(uintToStr(getHCIndex(addr, level, shift)));
}

void RAMImpl::invalidateHashes(uint32_t addr) {
  for (uint32_t i = 0; i < _numLevels; i++) {
    delHash(addr, i, true);
  }
}

void RAMImpl::updateHashes() {
  if (_rootHash != NULL)
    delete _rootHash;
  _rootHash = _hasher->createHash(updateSubtree(0, 0));
}

Bits RAMImpl::updateSubtree(uint32_t addr, uint32_t level)
{
  if ((addr << (_numLevels - level - 1)) >= DB_NUM_ADDRESSES) {
    std::cerr << "Returning _emptyHash: addr=" << addr << " level=" << level << std::endl;
    return _emptyHash; // replace nonexistent subtrees with a stub
  }

  Bits hash;

  if (getHash(addr, level, false, hash)) {
    return hash;
  }

  if (level == _numLevels - 1) {
    hash = _hasher->hash(get(addr));
  } else {
    Bits left = updateSubtree(addr << 1, level + 1);
    Bits right = updateSubtree((addr << 1) | 1, level + 1);
    hash = _hasher->hash(left, right);
  }

  putHash(addr, level, hash, false);
  return hash;
}
