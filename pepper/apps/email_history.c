#include "stdint.h"
#include "db.h"

/*
  Given a database of NUM_EMAILS emails,
  each with a 64-bit timestamp and two 64-bit integers identifying
  the sender and receiver,

  and as input a minimum time t_1 and a 64-bit integer identifying
  a person of interest,

  return the first MAX_RESULTS emails which have timestamp at least t_1
  and where the sender is the person of interest, and the first
  MAX_RESULTS emails which have timestamp at least t_1 where the receiver
  is the person of interest.
  
  Warning - costs projected to be quadratic in the value of MAX_RESULTS.
*/

//Constants
#define MAX_RESULTS 5
#define NUM_EMAILS 32

typedef uint64_t timestamp_t;
typedef uint64_t person_t;

struct email {
  timestamp_t timestamp;
  person_t sender;
  person_t receiver;
};

struct resultList {
  int length;
  struct email emails[MAX_RESULTS];
};

struct emailDB {
  struct email emails[NUM_EMAILS];
};

void resultList_clear(struct resultList* r){
  r->length = 0;
}

void email_copy(struct email* dst, struct email* src){
  dst->timestamp = src->timestamp;
  dst->sender = src->sender;
  dst->receiver = src->receiver;
}

void resultList_add(struct resultList* r, struct email* toAdd){
  if (r->length < MAX_RESULTS){
    int i;
    for(i = 0; i < MAX_RESULTS; i++){
      if (i == r->length){
        email_copy(&(r->emails[i]), toAdd);
      }
    }
    r->length++;
  }
}

struct In {
  hash_t digest_of_db;
  timestamp_t t_1;
  person_t search;
};

struct Out {
  struct resultList was_sender;
  struct resultList was_receiver;
};


int compute(struct In *input, struct Out *output) {
  struct emailDB db;

  hashget(&db, &(input->digest_of_db));

  resultList_clear(&(output->was_sender));
  resultList_clear(&(output->was_receiver));
  
  int i;
  for(i = 0; i < NUM_EMAILS; i++){
    struct email* e = &(db.emails[i]);
    if (e->timestamp >= input->t_1){
      if (e->sender == input->search){
        resultList_add(&(output->was_sender), e);
      }
      if (e->receiver == input->search){
        resultList_add(&(output->was_receiver), e);
      }
    }
  }
}
