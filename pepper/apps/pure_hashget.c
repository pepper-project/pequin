#include <stdint.h>
#include <db.h>
#include "pure_hashget.h"

void compute(struct In *input, struct Out *output){
  uint32_t i;
  for (i = 0; i < NUM_OF_BLOCKS; i++) {
    hashget(&(output->blocks[i]), &(input->hashes[i]));
  }
  return 0;
}

