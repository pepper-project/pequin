#include "stdint.h"
#include "db.h"

/*
  Given a database of NUM_DB_SNPS snp identifiers,
  which holds a vector which is the concatenation of 
  the snp lists of a number of individuals,

  and as input an snp identifier to search for,

  returns how many times the search identifier occurs in the database
*/

//Constants
#define NUM_DB_SNPS 32

typedef int64_t snp_t;

struct SNP_DB {
  snp_t snps[NUM_DB_SNPS];
  int num_individuals;
};

struct In {
  hash_t digest_of_db;
  snp_t search;
};

struct Out {
  int matched_individuals;
  int total_individuals;
};

int compute(struct In *input, struct Out *output) {
  struct SNP_DB db;

  hashget(&db, &(input->digest_of_db));

  int i;
  output->matched_individuals = 0;
  for(i = 0; i < NUM_DB_SNPS; i++){
    if (db.snps[i] == input->search){
      output->matched_individuals++;
    }
  }
  output->total_individuals = db.num_individuals;
}
