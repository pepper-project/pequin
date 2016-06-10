#include <stdint.h>
#include <db.h>

#define SIZE 300 

struct In {int32_t data[SIZE];};
struct Out {hash_t hash;};

void compute(struct In *input, struct Out *output){
  int i=0;
  /*
  for (i=0; i<SIZE-1; i++)
    input->data[i] = input->data[i] * input->data[i+1];
  */
  hashput(&(output->hash), input);
  
  hashget(input, &(output->hash));
}
