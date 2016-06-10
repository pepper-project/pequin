#include <fix_t.h>
#include <stdint.h>

#define INPUT_SIZE 4

struct In {
  fix_t data[INPUT_SIZE];
};

struct Out {
  fix_t result;
};

int compute(struct In* input, struct Out* output){
  int i;
  //output->result = fix_sqrt(input->data[1]);
  //output->result = fix_mul(input->data[0], input->data[1]);
  //output->result = fix_add(input->data[0], input->data[1]);
  output->result = fix_div(input->data[0], input->data[1]);
}
