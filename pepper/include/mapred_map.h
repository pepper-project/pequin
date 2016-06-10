#ifndef MAPRED_MAP_H
#define MAPRED_MAP_H
#include <stdint.h>
#include "db.h"

// input to a mapper
typedef struct _In {
#ifdef HAS_SERVERSIDE_INPUT
  hash_t serverside_in_d;
#endif
  hash_t clientside_in_d;
} In;

// output of a mapper
typedef struct _Out {
  hash_t d[NUM_REDUCERS];
} Out;

// main function to call the mapper
inline void compute(In *input, Out *output) {
  MapperIn mapper_input;
  MapperOut mapper_output;
  int i;

  // read input exogeneously using the client-provided digest
#ifdef HAS_SERVERSIDE_INPUT
  //serverside must come first in MapperIn struct, and in input generated
  //by the input generator.
  hashget(&mapper_input.serverside_in, &input->serverside_in_d);
  hashget(&mapper_input.clientside_in, &input->clientside_in_d);
#else
  hashget(&mapper_input, &input->clientside_in_d);
#endif

  // call the mapper with the actual input
  //use of 2 colons to get the map in global scope is C++ ugliness
  ::map(&mapper_input, &mapper_output);

  // provide digests to the client
  for (i=0; i<NUM_REDUCERS; i++) {
    hashput(&(output->d[i]), &(mapper_output.output[i]));
  }
}
#endif
