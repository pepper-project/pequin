#include <stdint.h>
#include "mr_cov.h"

void reduce (ReducerIn *reducer_input, ReducerOut *reducer_output) {
  int i,j,k;

  //Sum all of the mappers' data
  ReducerChunkIn sum_in;

  //Zero out sum_in
  for(i = 0; i < NUM_VARS; i++){
    sum_in.sum_x[i] = 0;
    for(j = 0; j < NUM_VARS; j++){
      sum_in.sum_x_times_y[i][j] = 0;
    }
  }

  //Add all of the mappers outputs
  for(i = 0; i < NUM_MAPPERS; i++) {
    ReducerChunkIn* in = &reducer_input->input[i];
    //Add in's data to sum_in

    for(j = 0; j < NUM_VARS; j++){
      sum_in.sum_x[j] += in->sum_x[j];
      for(k = 0; k < NUM_VARS; k++){
	sum_in.sum_x_times_y[j][k] += in->sum_x_times_y[j][k];
      }
    }
  }

  //Compute output
  for(i = 0; i < NUM_VARS; i++){
    for(j = 0; j < NUM_VARS; j++){
      //Use formula cov(X,Y) = E(x*y) - E(x)*E(y)
      //This formula apparently has precision problems on fp, 
      //but it should be fine for integer computations
      reducer_output->scaled_cov[i][j] = 
	sum_in.sum_x_times_y[i][j]*NUM_DATAPOINTS_PER_MAPPER*NUM_MAPPERS -
	sum_in.sum_x[i]*sum_in.sum_x[j];
      //Result is a factor of NUM_VARS^2 too large.
    }
  }
}

// include this header _only_ after the above macros are defined
#include <mapred_red.h>
