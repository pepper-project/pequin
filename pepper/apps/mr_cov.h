#ifndef _MR_COV_H
#define _MR_COV_H

#define NUM_DATAPOINTS_PER_MAPPER 2500
#define NUM_VARS 10

#define NUM_MAPPERS 10
#define NUM_REDUCERS 1

typedef int16_t measure_t;
typedef int64_t summation_t;

// actual input to the mapper
typedef struct _MapperIn {
  measure_t data[NUM_DATAPOINTS_PER_MAPPER][NUM_VARS];
} MapperIn;

// actual output of the mapper
typedef struct _MapperChunkOut {
  summation_t sum_x[NUM_VARS];
  summation_t sum_x_times_y[NUM_VARS][NUM_VARS];
} MapperChunkOut;

typedef struct _MapperOut {
  MapperChunkOut output[NUM_REDUCERS];
} MapperOut;

typedef struct _ReducerChunkIn {
  summation_t sum_x[NUM_VARS];
  summation_t sum_x_times_y[NUM_VARS][NUM_VARS];
} ReducerChunkIn;

typedef struct _ReducerIn {
  ReducerChunkIn input[NUM_MAPPERS];
} ReducerIn;

typedef struct _ReducerOut {
  //Returns the covariance matrix, whose entries are a factor of
  //(NUM_DPS_PER_MAPPER*NUM_MAPPERS)^2 too large.
  summation_t scaled_cov[NUM_VARS][NUM_VARS];
} ReducerOut;

#endif
