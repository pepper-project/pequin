#ifndef GGH_HASH_H_
#define GGH_HASH_H_

#include <storage/hasher.h>

class GGHHash: public Hasher {
public:
	GGHHash();
	virtual ~GGHHash();

	virtual int getNumHashBits();
	virtual HashType* createHash(const Bits& hashBits);
	virtual Bits hash(const Bits& v);
	virtual Bits hash(const Bits& left, const Bits& right);
};

#endif /* GGH_HASH_H_ */
