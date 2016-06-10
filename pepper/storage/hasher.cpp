#include <storage/hasher.h>

HashType::HashType(const Bits& hashBits, int numElts) {
  int index = 0;
  for (int i = 0; i < numElts; i++) {
    mpz_class elt;
    for (int j = 0; j < HashType::FIELD_SIZE; j++) {
      bool bit = hashBits[index++];
      if (bit) {
        mpz_setbit(elt.get_mpz_t(), j);
      }
    }
    _fieldElts.push_back(elt);
  }
}

HashType::~HashType() {
}

const HashType::HashVec& HashType::GetFieldElts() {
  return _fieldElts;
}
