#include <common/utility.h>

#include <include/db.h>

#include <storage/exo.h>

//Uninitialized variables are in fact initialized to 0, standard libraries
//can take advantage of this non-standard-C behavior but user could shouldn't
//hash_t __NULL_HASH__ = {{0,0,0,0,0,0,0,0,0,0,0,0}};
//hash_t* NULL_HASH = &__NULL_HASH__;
/*
int hasheq(hash_t* a, hash_t* b){
  int i;
  int isEq = 1;
  for(i = 0; i < (DB_HASH_NUM_BITS/64); i++){
    if (a->bit[i] != b->bit[i]){
      isEq = 0;
    }
  }
  return isEq;
}
*/
