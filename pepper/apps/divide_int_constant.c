#include <stdint.h>
#define DIVISOR 0x4726
struct In { uint32_t a; };
struct Out { uint32_t amodp; };
void compute(struct In *input, struct Out *output){
  output->amodp = input->a % DIVISOR;
}
int main(int argc, char **argv){
  struct In input;
  struct Out output;
  input.a = 2;
  compute(&input, &output);
  printf("%d %% %d = %d\n", input.a, DIVISOR, output.amodp);
}
