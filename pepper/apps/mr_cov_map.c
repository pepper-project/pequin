#include <stdint.h>
#include "mr_cov.h"

void map (MapperIn *mapper_input, MapperOut *mapper_output) {
  int i, j, k;

  MapperChunkOut* out = &mapper_output->output[0];

  //Only one reducer.
  assert_zero(NUM_REDUCERS-1);

  //Zero out output
  for(i = 0; i < NUM_VARS; i++){
    out->sum_x[i] = 0;
    for(j = 0; j < NUM_VARS; j++){
      out->sum_x_times_y[i][j] = 0;
    }
  }

  //Iterate over data, computing sums
  for(i = 0; i < NUM_DATAPOINTS_PER_MAPPER; i++){
    for(j = 0; j < NUM_VARS; j++){
      out->sum_x[j] += mapper_input->data[i][j];
      for(k = 0; k < NUM_VARS; k++){
	out->sum_x_times_y[j][k] += mapper_input->data[i][j] * mapper_input->data[i][k];
      }
    }
  }
}

// include this header _only_ after the above are defined
#include <mapred_map.h>
