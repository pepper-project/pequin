#ifndef MERKLE_NULL_HASH_H_
#define MERKLE_NULL_HASH_H_

#include <storage/hasher.h>

class NullHash : public Hasher {
public:
	NullHash();
	virtual ~NullHash();

	// The output of the hash should be 256 bits to resist birthday attacks
	virtual int getNumHashBits();
	virtual HashType* createHash(const Bits& hashBits);
	virtual Bits hash(const Bits& v);
	virtual Bits hash(const Bits& left, const Bits& right);
};

#endif
