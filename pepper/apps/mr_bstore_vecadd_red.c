#include <stdint.h>
#define NUM_MAPPERS 1
#define NUM_REDUCERS 1

#define SIZE_INPUT 2
#define SIZE_OUTPUT 1

typedef struct _ReducerChunkIn {
  uint32_t input;
} ReducerChunkIn;

typedef struct _ReducerIn {
  ReducerChunkIn input[NUM_MAPPERS];
} ReducerIn;

typedef struct _ReducerOut {
  uint32_t output;
} ReducerOut;


// include this header _only_ after the above macros are defined
#include <mapred_red.h>

void reduce (ReducerIn *reducer_input, ReducerOut *reducer_output) {
  int i;
  reducer_output->output = 0;
  for(i = 0; i < NUM_MAPPERS; i++) {
    reducer_output->output = reducer_output->output + (reducer_input->input[i]).input;
  }
}
