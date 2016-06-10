#include <storage/null_hash.h>
#include <storage/hasher.h>

NullHash::NullHash() {
}

NullHash::~NullHash() {

}

int NullHash::getNumHashBits() {
  return HashType::FIELD_SIZE*2;
}

HashType* NullHash::createHash(const Bits& hashBits) {
  return new HashType(hashBits, 2);
}

Bits NullHash::hash(const Bits& v) {
  Bits hashBits(getNumHashBits());

  uint32_t start = 0;
  if (v.size() < hashBits.size()) {
    start = hashBits.size() - v.size();
  }

  for (uint32_t i = 0; i < hashBits.size(); i++) {
    bool bit = (i < start) ? false : v[i - start];
    hashBits[i] = bit;
  }

  return hashBits;
}

Bits NullHash::hash(const Bits& left, const Bits& right) {
  return left ^ right;
}
