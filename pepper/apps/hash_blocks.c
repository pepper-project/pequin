#include <stdint.h>
#include <db.h>

#define M 5376
#define SIZE_OF_UINT32 32

#define BLOCKLEN (M/SIZE_OF_UINT32)

struct In {
  uint32_t block[BLOCKLEN];
};

struct Out {
  hash_t hash;
};

void compute(struct In *input, struct Out *output){
  int i;
  hashput(&output->hash, input);
}
