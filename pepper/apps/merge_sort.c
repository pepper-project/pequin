#include <stdint.h>
#define MAX_SIZE 32

struct In {uint16_t input[MAX_SIZE];};
struct Out {uint16_t output[MAX_SIZE];};

void compute(struct In *input, struct Out *output){
  int left, right, rend;
  int i, j, k, m;
  int interval;

  for (interval = 1; interval < MAX_SIZE; interval *= 2 ) {
    for (left = 0; left + interval < MAX_SIZE; left += interval * 2 ) {
      right = left + interval;
      rend = right + interval;

      if (rend > MAX_SIZE) rend = MAX_SIZE;

      m = left; i = left; j = right;

      for (k = 0; k < 2 * interval; k++) {
        if (i < right && j < rend) {
          uint16_t x = input->input[i];
          uint16_t y = input->input[j];
          uint16_t smaller;
          if (x < y) {
            smaller = x;
            i++;
          } else {
            smaller = y;
            j++;
          }
          output->output[k + left] = smaller;
          m++;
        }
      }

      for (k = left; k < right; k++) {
        if (i < right) {
          output->output[m] = input->input[i];
          i++; m++;
        }
      }

      for (k = right; k < rend; k++) {
        if (j < rend) {
          output->output[m] = input->input[j];
          j++; m++;
        }
      }

      for (k = left; k < rend; k++) {
        input->input[k] = output->output[k];
      }
    }
  }
  for (i = 0; i < MAX_SIZE; i++) {
    output->output[i] = input->input[i];
  }
}


