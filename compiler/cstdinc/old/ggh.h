#ifndef _GGH_H
#define _GGH_H
#include <stdint.h>
#include <ggh_key.h>

typedef uint32_t bool;

struct Message {
  uint32_t msg[CHUNK_SIZE];
};

struct Digest {
  uint32_t digest[DIGEST_SIZE];
};

void to_digest(struct Message *m, struct Digest *d) {
  uint32_t i;
  uint32_t j;
  for (i=0; i<DIGEST_SIZE; i++) {
    d->digest[i] = 0;
    for (j=0; j<CHUNK_SIZE; j++) {
      d->digest[i] = d->digest[i] + ggh_key[i*CHUNK_SIZE+j] * m->msg[j];
    }
    d->digest[i] = d->digest[i] % q; 
  }
}

uint32_t check_digest(struct Message *m, struct Digest *d) {
  struct Digest d2;
  uint32_t i;
  uint32_t is_equal = 1;
  
  to_digest(m, &d2);
  for (i=0; i<DIGEST_SIZE; i++) {
    if (d2.digest[i] != d->digest[i])
      is_equal = 0;
  }
  return is_equal;
}
#endif
