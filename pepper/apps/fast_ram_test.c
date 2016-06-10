#include <stdint.h>

struct tt {int x[2];int y;};

struct In { uint16_t index; int array[4]; struct tt s_array[4]; int input1, input2;};
struct Out { int array[4]; struct tt s_array[4]; int output;};

int compute(struct In *input, struct Out *output) {
  uint16_t i = input->index % 4;
  output->array[i] = input->array[i] + input->index;
  output->s_array[i] = input->s_array[i];
  output->s_array[i].x[0] += input->index;
  output->s_array[i].x[1] += input->array[i] * input->index;
  output->s_array[i].y -= input->index;

  int *p;
  int a = input->input1;
  int b = input->input2;
  if (input->index > 1000) {
    p = &a;
  } else {
    p = &b;
  }

  output->output = *p;

  return 0;
}
