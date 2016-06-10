/**
Test functionality of a hashfree function, that 
takes a pointer to a hash and instructs the prover
to erase the data it has associated with the hash.

Note that hashfree can not be verified, it merely functions as an
execution hint to the prover.
**/
#include <stdint.h>
#include <db.h>

typedef int32_t state_t;

struct In {
  hash_t digest;
};

struct Out {
  int unused; //Empty structs aren't allowed.
};

int compute(struct In* input, struct Out* output){
  hashfree(&input->digest);
}

