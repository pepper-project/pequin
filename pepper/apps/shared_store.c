#include <stdint.h>
#include <db.h>

#define SIZE 1

struct In {uint32_t test;};
/*struct Out {uint8_t data[SIZE]; };*/
struct Out {uint32_t data;uint32_t data1;};
/*struct Out {hash_t hash; struct In data;};*/

void compute(struct In *input, struct Out *output){
  /*ramput(0, input);*/
  hash_t hash;
  ramget(&(hash), 0);
  ramget(&(output->data1), 1);
  /*ramget(output, 0);*/

  /*hash_t hash;*/
  /*hashput(&(hash), input);*/
  /*hashput(&(output->hash), input);*/
  hashget(&(output->data), &(hash));
  /*hashget(output, &(hash));*/
  /*output->success = 1;*/
}
