#ifndef FIX_T_H
#define FIX_T_H

#include <stdint.h>

typedef int32_t fix_t;

#define FIX_SCALE 0x10000
#define FIX_SCALE_LOG2 16
#define FIX_T_MIN -2147483648
#define FIX_T_MAX 2147483647

//Arithmetic routines do not overflow, and always truncate towards zero
fix_t fix_add(fix_t a, fix_t b){
  return a + b;
}
fix_t fix_mul(fix_t a, fix_t b){
  int64_t prod = ((int64_t)a) * b;
  return (fix_t)(prod >> FIX_SCALE_LOG2);
}
fix_t fix_div(fix_t a, fix_t b){
  int64_t quotient = ((int64_t)a) << FIX_SCALE_LOG2;
  quotient /= b;
  return (fix_t)(quotient);
}

// The result must be in the closed interval [0, 2^bits - 1]
uint64_t uint64sqrt(uint64_t a, int bits){
  uint64_t lo = 0;

  int i;
  for(i = bits - 1; i >= 0; i--){
    uint64_t mid = lo + (1UL << i);
    if (mid * mid > a){
      //Overshot
    } else {
      lo = mid;
    }
  }

  return lo;
}

fix_t fix_sqrt(fix_t a){
  //A should be nonnegative.
  uint64_t tosqrt = ((uint64_t) a) << FIX_SCALE_LOG2;
  return (fix_t)(uint64sqrt(tosqrt,(32 + FIX_SCALE_LOG2)/2));
}

fix_t int_to_fix(int32_t val){
  return (fix_t)(((uint64_t)val) << FIX_SCALE_LOG2);
}

int32_t fix_to_int(fix_t val){
  return (int32_t)(val >> FIX_SCALE_LOG2);
}

fix_t fix_ceil(fix_t val){
  val = fix_add(val, FIX_SCALE - 1);
  return int_to_fix(fix_to_int(val));
}

#endif //FIX_T_H
