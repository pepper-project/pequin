#include <stdint.h>
#include <db.h>

struct In { int addr;};
struct Out { int value; };

// An example that uses ramput and ramget
void compute(struct In *input, struct Out *output){
  int value = 1234;
  ramput(input->addr, &value); 
  ramget(&(output->value), input->addr);
}
