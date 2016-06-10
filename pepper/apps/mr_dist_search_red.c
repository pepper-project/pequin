#include "mr_dist_search.h"

void reduce (ReducerIn *reducer_input, ReducerOut *reducer_output) {
  int i, j;
  reducer_output->output = reducer_input->input[0].input[0];
  for(i = 0; i < NUM_MAPPERS; i++) {
    for(j=0; j < SIZE_HAYSTACK/SIZE_NEEDLE; j++) {
      if (reducer_input->input[i].input[j] < reducer_output->output)
        reducer_output->output = reducer_input->input[i].input[j];
    }
  }
}

// include this header _only_ after the above macros are defined
#include <mapred_red.h>
