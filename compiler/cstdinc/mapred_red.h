#ifndef MAPRED_RED_H
#define MAPRED_RED_H
#include <stdint.h>
#include <db.h>

typedef struct _In {
  hash_t d[NUM_MAPPERS];
} In;

typedef struct _Out {
  hash_t d;
} Out;

//==========================================================

// main function to call the reducer 
inline void compute(In *input, Out *output) {
  ReducerIn reducer_input;
  ReducerOut reducer_output;
  int i;
  
  // read input exogeneously using the client-provided digest
  for (i=0; i<NUM_MAPPERS; i++) {
    hashget(&reducer_input.input[i], &input->d[i]);
  }

  // call the reduce
  reduce(&reducer_input, &reducer_output);

  // store the output of the reducer
  hashput(&output->d, &reducer_output);
}
#endif
