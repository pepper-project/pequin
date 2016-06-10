#include <stdint.h>
#include <db.h>
#include <hashmap.h>

#define HASHMAP_SIZE 8
#define NUM_PUTS 8

struct put {uint32_t key; uint32_t value; };

struct In {struct put puts[NUM_PUTS]; uint32_t lookup; };
struct Out {uint32_t value; };

//========================================================================

uint32_t hash(uint32_t value){
  return (value * 37) & (HASHMAP_SIZE - 1);
}

/*
  Insert NUM_PUTS key / value pairs into the map,
  and then return the value associated with lookup,
  using the RAM implementation.
*/
void compute(struct In *input, struct Out *output){
  int i;

  hashmap_t hashmap;
  hashmap_init(&hashmap, 0, HASHMAP_SIZE); ;//Build a hashmap at ram address 0, of HASHMAP_SIZE ram addresses.

  for(i = 0; i < NUM_PUTS; i++){
    struct put toPut = input->puts[i];
    hashmap_put(&hashmap, &(toPut.key), hash(toPut.key), &(toPut.value));
  }

  {
    uint32_t got;
    hashmap_get(&hashmap, &got, &(input->lookup), hash(input->lookup));
    output->value = got;
  }
}
