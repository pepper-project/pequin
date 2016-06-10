#include <stdint.h>

#define NUM_ROWS_PER_MAPPER 10
#define NUM_MAPPERS 10
#define NUM_REDUCERS 1

typedef uint32_t num_t;

typedef struct _ReducerChunkIn {
  num_t product_part [NUM_ROWS_PER_MAPPER];
} ReducerChunkIn;

typedef struct _ReducerIn {
  ReducerChunkIn input[NUM_MAPPERS];
} ReducerIn;

typedef struct _ReducerOut {
  num_t product [NUM_ROWS_PER_MAPPER * NUM_MAPPERS];
} ReducerOut;

// include this header _only_ after the above macros are defined
#include <mapred_red.h>

void reduce (ReducerIn *reducer_input, ReducerOut *reducer_output) {
  int i,j,k;
  j = 0;
  for(i = 0; i < NUM_MAPPERS; i++) {
    for(k = 0; k < NUM_ROWS_PER_MAPPER; k++){
      reducer_output->product[j] = (reducer_input->input[i]).product_part[k];
      j++;
    }
  }
}
