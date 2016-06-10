#include "stdint.h"

struct In {
  uint32_t a;
};

struct Out {
  uint32_t sqrt;
};

//Binary search for the integer square root.
uint32_t uintsqrt(uint32_t x){
  uint32_t lo = 0;

  int i;
  uint32_t bit = 0x8000;
  for(i = 0; i < 16; i++){
    uint32_t mid = lo + bit;

    if (mid * mid > x){
      //Overshot. Don't add bit.
    } else {
      lo = mid;
    }

    bit = bit / 2;
  }

  return lo;

  /*
  //Alternative algorithm used in 
  //Privacy-preserving ridge regression on hundreds of millions of
  //records, Valeria Nikolaenko
  //Udi Weinsberg, Stratis Ioannidis, Marc Joye, Dan Boneh, Nina Taft
  //Not as efficient (in terms of constraints) as above algorithm.

  uint32_t e = 0x40000000;
  int i;
  uint32_t x = x_orig;
  uint32_t r = 0;
  for(i = 0; i < 16; i++){
    if (x >= r + e){
      x -= r + e;
      r = (r >> 1) + e;
    } else {
      r = r >> 1;
    }
    e = e / 4;
  }
  return r;
  */
}

int compute(struct In* input, struct Out* output){
  output->sqrt = uintsqrt(input->a);
}
