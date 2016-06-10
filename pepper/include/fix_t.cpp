#include <stdint.h>
#include "fix_t.h"
#include <iostream>

#define DEBUG_FIX_T 0

//Arithmetic routines do not overflow, and always truncate towards zero
fix_t fix_add(fix_t a, fix_t b){
  return a + b;
}
fix_t fix_mul(fix_t a, fix_t b){
  int64_t prod = ((int64_t)a) * b;
#if DEBUG_FIX_T == 1
    std::cout << "Multiply " << a << " " << b << std::endl;
#endif
  return (fix_t)(prod >> FIX_SCALE_LOG2);
}
fix_t fix_div(fix_t a, fix_t b){
  if (b == 0){
    std::cout << "ERROR: Fix_t division " << a << " by zero " << std::endl;
    return 0;
  }

  int64_t quotient = ((int64_t)a) << FIX_SCALE_LOG2;
  quotient /= b;

#if DEBUG_FIX_T == 1
  std::cout << "Integer dividing " << a << " by " << b << " result " <<
  quotient<< std::endl;
#endif
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

#if DEBUG_FIX_T == 1
    std::cout << "sqrt " << a << " " << lo << std::endl;
#endif
  return lo;
}

fix_t fix_sqrt(fix_t a){
  //A should be nonnegative.
  uint64_t tosqrt = ((uint64_t) a) << FIX_SCALE_LOG2;
  return (fix_t)(uint64sqrt(tosqrt,(32 + FIX_SCALE_LOG2)/2));
}
