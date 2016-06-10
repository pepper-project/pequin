#include "mr_substring_search.h"

void reduce (ReducerIn *reducer_input, ReducerOut *reducer_output) {
  int i, j;
  int32_t* position = reducer_output->position;
  for(i = 0; i < NUM_NEEDLES; i++) {
    position[i] = -1;
  }
  for(i = 0; i < NUM_MAPPERS; i++) {
    for(j = 0; j < NUM_NEEDLES; j++){
      int found = reducer_input->input[i].position[j];
      if (found != -1){
        position[j] = found; //Finds last occurrence, if there are multiple occurrences
      }
    }
  }
}

// include this header _only_ after the above macros are defined
#include <mapred_red.h>
