#include "mr_substring_search.h"

void map (MapperIn *mapper_input, MapperOut *mapper_output) {
#if CURRENT_IMPL == BOOLEAN_IMPL
  int32_t* position = mapper_output->output[0].position;
  int32_t i,j,k;
  int32_t needle;

  for(i = 0; i < NUM_NEEDLES; i++){
    position[i] = -1;
  }

  assert_zero(NUM_REDUCERS - 1);

  for(i = 0; i < SIZE_HAYSTACK - (SIZE_NEEDLE - 1); i++){
    for(needle = 0; needle < NUM_NEEDLES; needle++){
      int match_target = 0;
      int match_counter = 0;
      for(j = 0; j < SIZE_NEEDLE; j++){
        for(k = 0; k < 2; k++){
          match_counter+=
          (mapper_input->clientside_in.needle[needle][j*2 + k] ==
          mapper_input->serverside_in.haystack[(i+j)*2 + k]
          );
          match_target++;
        }
      }
      if (match_counter == match_target){
        position[needle] = i;
      }
    }
  }
#endif
#if CURRENT_IMPL == STD_IMPL
  int32_t* position = mapper_output->output[0].position;
  int32_t i, j;
  int32_t needle;
  uint64_t needles_as_ints [NUM_NEEDLES];
  uint64_t needle_checking_window = 0;
  //Only one reducer
  assert_zero(NUM_REDUCERS - 1);
  //Needle cannot exceed 64 bits
  assert_zero(SIZE_NEEDLE > 32);
  //Haystack cannot be shorter than needle.
  assert_zero(SIZE_HAYSTACK < SIZE_NEEDLE);

  //Convert needles to windows
  for(needle = 0; needle < NUM_NEEDLES; needle++){
    position[needle] = -1;

    needles_as_ints[needle] = 0;
    for(i = 0; i < SIZE_NEEDLE; i++){
      j = 0;
      int32_t i_base = (i + j) / NUCS_PER_INT;
      int32_t i_offset = (i + j) % NUCS_PER_INT;

      needles_as_ints[needle] |=
      ((mapper_input->clientside_in.needle[needle][i_base] >>
      (i_offset*2)) & 3ULL) << i*2;
    }
  }

  for(i = 0; i < SIZE_HAYSTACK - (SIZE_NEEDLE - 1); i++){
    //Make window (Expected expansion of this for loop is 1 constraint)
    needle_checking_window = 0;
    for(j = 0; j < SIZE_NEEDLE; j++){
      int32_t i_base = (i + j) / NUCS_PER_INT;
      int32_t i_offset = (i + j) % NUCS_PER_INT;

      needle_checking_window |=
      ((mapper_input->serverside_in.haystack[i_base] >>
      (i_offset*2)) & 3ULL) << j*2;
    }
    for(needle = 0; needle < NUM_NEEDLES; needle++){
    //Determine whether needle #needle matches here
      //printf("Needle %d Window %d", needle_checking_window,
      /*needles_as_ints[needle]);*/
      if (needle_checking_window == needles_as_ints[needle]){
        position[needle] = i;
      }
    }
  }

#endif
#if CURRENT_IMPL == WORD_ALIGNED_IMPL
  int32_t* position = mapper_output->output[0].position;
  int32_t i, j, k;
  for(i = 0; i < NUM_NEEDLES; i++){
    position[i] = -1;
  }
  assert_zero(NUM_REDUCERS - 1);
  for(i = 0; i < SIZE_HAYSTACK_INTS - (SIZE_NEEDLE_INTS - 1); i++){
    for(j = 0; j < NUM_NEEDLES; j++){
      int match_target = 0;
      int match_counter = 0;
      for(k = 0; k < SIZE_NEEDLE_INTS; k++){
        match_counter += (mapper_input->clientside_in.needle[j][k]
            == mapper_input->serverside_in.haystack[i+k]);
        match_target ++;
      }
      if (match_target == match_counter){
        position[j] = i; //Finds last occurrence, if there are multiple occurrences
      }
    }
  }
#endif
}

// include this header _only_ after the above are defined
#include <mapred_map.h>
