#include <stdint.h>
#include <db.h>

#define NUM_BITS_TO_GET 128
#define NUM_INTS_TO_GET (NUM_BITS_TO_GET)/32

struct In { void placeholder; };
struct Out {uint32_t result[NUM_INTS_TO_GET]; };

/*
  Microbenchmark to measure the cost of hashget.
*/
void compute(struct In *input, struct Out *output){
  hash_t digest = *NULL_HASH;
  hashget(output, &digest);
}
