#include <stdint.h>

struct In {
  uint32_t a;
};

struct Out {
  uint32_t loga;
};

//Returns floor(log2(a)), only works for nonzero a.
uint32_t uintlog2(uint32_t a){
  int bits[32];
  int i;
  for(i = 0; i < 32; i++){
    bits[i] = (a & 1) != 0;
    a >>= 1;
  }
  uint32_t log = 0;
  int found = 0;
  for(i = 31; i>=1; i--){
    if (!found){
      if (bits[i]){
        log = i;
        found = 1;
      }
    }
  }
  return log;
}

void compute(struct In* input, struct Out* output){
  output->loga = uintlog2(input->a); 
}
