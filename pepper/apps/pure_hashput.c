#include <stdint.h>
#include <db.h>
#include "pure_hashput.h"

void compute(struct In *input, struct Out *output){
  uint32_t i;
  for (i = 0; i < NUM_OF_BLOCKS; i++) {
    hashput(&(output->hashes[i]), &(input->blocks[i]));
  }
  return 0;
}

