#include <stdint.h>
#define NUM_MAPPERS 1
#define NUM_REDUCERS 1

#define SIZE_INPUT 5
#define SIZE_OUTPUT 1

// actual input to the mapper
typedef struct _MapperIn {
  uint32_t input[SIZE_INPUT];
} MapperIn;

// actual output of the mapper
typedef struct _MapperChunkOut {
  uint32_t output[SIZE_OUTPUT];
} MapperChunkOut;

typedef struct _MapperOut {
  MapperChunkOut output[NUM_REDUCERS];
} MapperOut;


// include this header _only_ after the above are defined
#include <mapred_map.h>

void map (MapperIn *mapper_input, MapperOut *mapper_output) {
  int i;
  for(i = 0; i < NUM_REDUCERS; i++) {
    (mapper_output->output[i]).output[0] = mapper_input->input[i];
  }
}
