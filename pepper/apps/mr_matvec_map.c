#include <stdint.h>

#define NUM_VARS 10
#define NUM_ROWS_PER_MAPPER 10
#define NUM_MAPPERS 10
<<<<<<< HEAD
#define NUM_REDUCERS 1

typedef uint32_t num_t;
=======
#define NUM_REDUCERS 10

#define m 10
#define n 10
>>>>>>> update size of DB for RO queries

// actual input to the mapper
typedef struct _MapperIn {
  num_t matrix [NUM_ROWS_PER_MAPPER][NUM_VARS];
  num_t vector [NUM_VARS];
} MapperIn;

// actual output of the mapper
typedef struct _MapperChunkOut {
  num_t product [NUM_ROWS_PER_MAPPER];
} MapperChunkOut;

typedef struct _MapperOut {
  MapperChunkOut output[NUM_REDUCERS];
} MapperOut;

// include this header _only_ after the above are defined
#include <mapred_map.h>

void map (MapperIn *mapper_input, MapperOut *mapper_output) {
  int i,j;
  for(i = 0; i < NUM_ROWS_PER_MAPPER; i++) {
    (mapper_output->output[0]).product[i] = 0;
    for (j=0; j < NUM_VARS; j++){
      (mapper_output->output[0]).product[i] += mapper_input->matrix[i][j] * mapper_input->vector[j];
    }
  }
}
