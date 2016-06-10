#include <stdint.h>
#include <db.h>

struct In { uint32_t addr;};
struct Out { int value; };

// An example that uses ramput_hybrid and ramget_hybrid

void compute(struct In *input, struct Out *output){
  int value = 1234;
  ramput_hybrid(input->addr, &value); 
  ramget_hybrid(&(output->value), input->addr);
}
