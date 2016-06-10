#include "mr_f2.h"

void reduce (ReducerIn *reducer_input, ReducerOut *reducer_output) {
  int i;
  reducer_output->output = 0;
  for(i = 0; i < NUM_MAPPERS; i++) {
    reducer_output->output = reducer_output->output + (reducer_input->input[i]).input;
  }
}

// include this header _only_ after the above macros are defined
#include <mapred_red.h>
