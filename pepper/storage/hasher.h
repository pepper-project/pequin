#ifndef MERKLE_HASH_H_
#define MERKLE_HASH_H_

#include <vector>
#include <gmp.h>
#include <gmpxx.h>
#include <stdint.h>
#include <storage/storage.h>

class HashType {
public:
	static const int FIELD_SIZE = 64;


	//A hash of a value spans multiple field elements.
	typedef std::vector<mpz_class> HashVec;

	HashType(const Bits& hashBits, int numElts);
	virtual ~HashType();

	const HashVec& GetFieldElts();

protected:
	HashVec _fieldElts;
};

class Hasher {
public:
	Hasher() {}
	virtual ~Hasher() {}

	virtual int getNumHashBits() = 0;
	virtual HashType* createHash(const Bits& hash) = 0;
	virtual Bits hash(const Bits& v) = 0;
	virtual Bits hash(const Bits& left, const Bits& right) = 0;
};

#endif
