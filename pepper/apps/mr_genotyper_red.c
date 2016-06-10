#include <stdint.h>
#include "mr_genotyper.h"

void reduce (ReducerIn *reducer_input, ReducerOut *reducer_output) {
  int i,j,k;

  int outp = 0;
  for(i = 0; i < NUM_MAPPERS; i++){
    for(j = 0; j < NUM_LOCI_PER_MAPPER; j++){
      reducer_output->calls[outp] = reducer_input->input[i].calls[j];
      outp++;
    }
  }
}

// include this header _only_ after the above macros are defined
#include <mapred_red.h>
