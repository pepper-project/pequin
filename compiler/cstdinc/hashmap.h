#ifndef _HASHMAP_H
#define _HASHMAP_H   1
#include <stdint.h>

//Maximum number of collisions allowed per hash value
#define HASHMAP_MAX_COLLISIONS 3

typedef uint32_t hashmap_hash_t;
typedef uint32_t hashmap_val_t;
typedef uint32_t hashmap_key_t;
typedef struct key_value_pair_t {hashmap_key_t key; hashmap_val_t value; } key_value_pair_t;
typedef struct collision_list_t {key_value_pair_t
  pairs[HASHMAP_MAX_COLLISIONS]; uint32_t size; } collision_list_t;
typedef struct hashmap_t {uint32_t memLoc; uint32_t size; } hashmap_t;

//Library of functions modifying a hashmap based on RAM.

void hashmap_put_(hashmap_t* h, uint32_t address, collision_list_t* c){
  ramput(h->memLoc + address, c);
}

void hashmap_get_(hashmap_t* h, collision_list_t* c, uint32_t address){
  ramget(c, h->memLoc + address);
}

/**
  NOTE: size must be compile-time resolvable!
**/
void hashmap_init(hashmap_t* h, uint32_t memLoc, uint32_t size){
    uint32_t i;
    collision_list_t data;
    data.size = 0; 

    h->memLoc = memLoc;
    h->size = size;
    
    for(i = 0; i < h->size; i++){
      hashmap_put_(h, i, &data);
    }
}

void hashmap_put(hashmap_t* h, hashmap_key_t* key, hashmap_hash_t address, hashmap_val_t* value){
  uint32_t i;
  collision_list_t list;

  hashmap_get_(h, &list, address);
  //Future puts should override old ones.
  for(i = 0; i < HASHMAP_MAX_COLLISIONS; i++){
    if (i == list.size){
      list.pairs[i].key = *key;
      list.pairs[i].value = *value;
    }
  }
  //Assume we found an open slot to write in.
  list.size = i+1;
  hashmap_put_(h, address, &list);
}

void hashmap_get(hashmap_t* h, hashmap_val_t* value, hashmap_key_t* key, hashmap_hash_t address){
  uint32_t i;
  collision_list_t list;

  hashmap_get_(h, &list, address);
  //Future puts should override old ones.
  for(i = 0; i < HASHMAP_MAX_COLLISIONS && i < list.size; i++){
    if (list.pairs[i].key == *key){
      *value = list.pairs[i].value;
    }
  }
}

#endif /* hashmap.h */
