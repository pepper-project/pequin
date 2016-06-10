#ifndef RAM_IMPL_H_
#define RAM_IMPL_H_

#include <vector>
#include <map>
#include <utility>
#include <kcprotodb.h>
#include <kccachedb.h>

#include <storage/storage.h>
#include <storage/merkle_ram.h>


class Hasher;
class HashBlockStore;

class RAMImpl : public MerkleRAM {
public:
	RAMImpl(HashBlockStore* blockStore);
	virtual ~RAMImpl();

	virtual Bits get(uint32_t addr);
	virtual void put(uint32_t addr, const Bits& val);

	virtual HashType* getRootHash();
	virtual int getNumHashBits();
	virtual bool getSiblingHash(uint32_t index, uint32_t level, Bits& hash);

protected:
	HashBlockStore* _blockStore;

	uint32_t _numLevels;
	Hasher* _hasher;
	HashType* _rootHash;
	Bits _emptyHash;

	kyotocabinet::ProtoHashDB _hashCache;

	virtual void updateHashes();
	virtual size_t getHCIndex(uint32_t index, uint32_t level, bool shift);
	virtual bool getHash(uint32_t index, uint32_t level, bool shift, Bits& hash);
	virtual bool getHash(uint32_t cacheIndex, Bits& hash);
	virtual void putHash(uint32_t index, uint32_t level, const Bits& , bool shift);
	virtual void delHash(uint32_t index, uint32_t level, bool shift);
	virtual void invalidateHashes(uint32_t index);
	virtual Bits updateSubtree(uint32_t index, uint32_t level);
};

#endif
