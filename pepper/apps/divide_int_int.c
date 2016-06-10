#include <stdint.h>

typedef int32_t num_t;

struct In { num_t a; num_t b; };
struct Out { num_t amodb; };
void compute(struct In *input, struct Out *output){
  output->amodb = input->a % input->b;
}
int main(int argc, char **argv){
  struct In input;
  struct Out output;
  input.a = 51;
  input.b = 7;
  compute(&input, &output);
  printf("%d\n", output.amodb);

  uint32_t us = 4;
  int32_t s = -3;
  printf("%d = %d %% %d\n", us % s, us, s);
}
