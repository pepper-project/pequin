#ifndef MERKLE_RAM_H_
#define MERKLE_RAM_H_

#include <stdint.h>
#include <storage/storage.h>

class HashType;

class MerkleRAM {
public:
	MerkleRAM() {}
	virtual ~MerkleRAM() {}

	virtual Bits get(uint32_t addr) = 0;
	virtual void put(uint32_t addr, const Bits& val) = 0;

	virtual HashType* getRootHash() = 0;
	virtual int getNumHashBits() = 0;
	virtual bool getSiblingHash(uint32_t index, uint32_t level, Bits& hash) = 0;
};

#endif
