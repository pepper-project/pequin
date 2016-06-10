#include "mr_dist_search.h"

void map (MapperIn *mapper_input, MapperOut *mapper_output) {
  int i, j;
  int k=0;
  for (i=0; i<SIZE_HAYSTACK/SIZE_NEEDLE; i++) {
    mapper_output->output[0].output[i] = 0;
    for (j=0; j<SIZE_NEEDLE; j++) {
      mapper_output->output[0].output[i] = mapper_output->output[0].output[i] + (mapper_input->haystack[k] - mapper_input->needle[j]) * (mapper_input->haystack[k] - mapper_input->needle[j]);
      k = k + 1;
    }
  }
}

// include this header _only_ after the above are defined
#include <mapred_map.h>
