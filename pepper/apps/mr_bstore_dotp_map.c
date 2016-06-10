#include "mr_bstore_dotp.h"

void map (MapperIn *mapper_input, MapperOut *mapper_output) {
  int i;
  (mapper_output->output[0]).output = 0;
  for(i = 0; i < SIZE_INPUT; i++) {
    (mapper_output->output[0]).output += mapper_input->vec_a[i] * mapper_input->vec_b[i];
  }
}

// include this header _only_ after the above are defined
#include <mapred_map.h>
