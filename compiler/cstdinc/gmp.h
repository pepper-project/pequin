#ifndef GMP_H
#define GMP_H

#include <stdint.h>

const int LIMBS_IN_FIELD = 7;
const int MPZ_LENGTH = 72;

struct mpz_t {
  uint32_t limbs [MPZ_LENGTH];
};

typedef struct mpz_t mpz_t[1];

void mpz_init(mpz_t a){
  //Do nothing
}

void mpz_clear(mpz_t a){
  //Do nothing
}

void mpz_set_si(mpz_t a, int64_t num){
  uint64_t val = (uint64_t)(num);
  a->limbs[0] = val & 0xFFFFFFFF;
  a->limbs[1] = (val >> 32) & 0xFFFFFFFF;
  if ((val >> 63) & 1){
    int i;
    for(i = 2; i < MPZ_LENGTH; i++){
      a->limbs[i] = 0xFFFFFFFF;
    }
  } else {
    int i;
    for(i = 2; i < MPZ_LENGTH; i++){
      a->limbs[i] = 0;
    }
  }
}

void mpz_set_str(mpz_t c, char* input, int radix){
  __INT_STRING_TO_BITS(&c->limbs, input, radix);
}

void mpz_set(mpz_t c, mpz_t a){
  int i;
  for(i = 0; i < MPZ_LENGTH; i++){
    c->limbs[i] = a->limbs[i];
  }
}



int64_t mpz_get_si(mpz_t a){
  //Return the lower 63 bits of the number, where the sign bit is the
  //sign of the number
  uint32_t upper = (a->limbs[1] & 0x7FFFFFFF) |
      (a->limbs[MPZ_LENGTH-1] & 0x80000000);
  return (int64_t)((((uint64_t)upper) << 32) | a->limbs[0]);
}

void mpz_neg(mpz_t c, mpz_t a){
  int i;
  for(i = 0; i < MPZ_LENGTH; i++){
    c->limbs[i] = ~a->limbs[i];
  }
  mpz_t one;
  mpz_set_si(one, 1);
  mpz_add(c, c, one);
}

void mpz_add(mpz_t c, mpz_t a, mpz_t b){
  //c can be equal to a or b.
  int i;
  int carry = 0;
  for(i = 0; i < MPZ_LENGTH; i++){
    uint64_t sum = ((uint64_t)a->limbs[i]) + b->limbs[i] + carry;
    carry = (sum >> 32) &1;
    c->limbs[i] = sum & 0xFFFFFFFF;
  }
}

//Add b * 2^(offset_limbs*32) to a
void mpz_add_(mpz_t c_, mpz_t a, mpz_t b, int offset_limbs){
  mpz_t c;
  mpz_set(c, a);
  int i;
  int carry = 0;
  for(i = offset_limbs; i < MPZ_LENGTH; i++){
    uint64_t sum = ((uint64_t)c->limbs[i]) + b->limbs[i - offset_limbs] + carry;
    carry = (sum >> 32) &1;
    c->limbs[i] = sum & 0xFFFFFFFF;
  }
  mpz_set(c_, c);
}



void mpz_sub(mpz_t c_, mpz_t a, mpz_t b){
  //Use a temp
  mpz_t c;
  mpz_neg(c, b);
  mpz_add(c, c, a);
  mpz_set(c_, c);
}

void mpz_mul(mpz_t c, mpz_t a, mpz_t b){
  //mpz_mul_karatsuba(c, a, b, MPZ_LENGTH);
  mpz_mul_(c, a, b, MPZ_LENGTH);
}

void mpz_mul_karatsuba(mpz_t c, mpz_t a, mpz_t b, int size){
  if (1){
    mpz_mul_(c, a, b, size);
  } else {
    if (size % 2){
      assert_zero(1); //Cannot karatsuba if size is not even
    }
    mpz_t x0, x1, y0, y1; //Library function, hence 0 init.
    int i;
    for(i = 0; i < size / 2; i++){
      x0->limbs[i] = a->limbs[i];
      x1->limbs[i] = a->limbs[i+size/2];
      y0->limbs[i] = b->limbs[i];
      y1->limbs[i] = b->limbs[i+size/2];
    }
    mpz_t z2, z1, z0;
    mpz_mul_karatsuba(z2, x1, y1, size/2);
    mpz_mul_karatsuba(z0, x0, y0, size/2);

    mpz_t sumx, sumy;
    mpz_add(sumx, x0, x1);
    mpz_add(sumy, y0, y1);
    mpz_mul_karatsuba(z1, sumx, sumy, size/2);
    mpz_sub(z1, z1, z2);
    mpz_sub(z1, z1, z0);

    mpz_set(c, z0);
    mpz_add_(c, c, z1, size/2);
    mpz_add_(c, c, z2, size);
  }
}

void mpz_mul_(mpz_t c_, mpz_t a, mpz_t b, int size){
  //We build c_ incrementally, so store into a temp:
  int i;
  mpz_t c; //Zero initialized
  //Works but slow.
  /*
  for(i = size - 1; i >= 0; i--){
    int j;
    for(j = 31; j >= 0; j--){
      mpz_add(c, c, c);
      if ((b->limbs[i] >> j) & 1){
        mpz_add(c, c, a);
      } else {
      }
    }
  }
  */

  //Can only multiply 3 limbs at a time this way
  //mpz_mul_infield(c, a, 0, b, 0, LIMBS_IN_FIELD/2);

  /*
  for(i = 0; i < size; i++){
    int j;
    uint32_t carry = 0;
    for(j = 0; j < size && j + i < MPZ_LENGTH; j++){
      uint64_t prod = ((uint64_t)a->limbs[j]) * b->limbs[i] +
                  carry +
                  c->limbs[j + i];
      //Wow, that is lucky.
      carry = (prod >> 32) & 0xFFFFFFFF;
      c->limbs[j + i] = prod & 0xFFFFFFFF;
    }
  }
  */

  //Optimized version of above: take limbs N at a time,
  //where
  /*
  int N = LIMBS_IN_FIELD/2;
  //Currently, we assume size and MPZ_LENGTH are multiples of N.
  for(i = 0; i < size; i += N){
    int j;
    mpz_t carry; //N nonzero limbs
    for(j = 0; j < size && j + i < MPZ_LENGTH; j += N){
      mpz_t prod; //inits to 0
      mpz_mul_infield(prod, a, j, b, i, N);
      mpz_add(prod, prod, carry);
      int k;
      for(k = 0; k < N; k++){
        carry->limbs[k] = c->limbs[j+i+k];
      }
      mpz_add(prod, prod, carry);
      //prod fits in 2N limbs
      for(k = 0; k < N; k++){
        if (k + N < MPZ_LENGTH){
          carry->limbs[k] = prod->limbs[k+N];
        }
        c->limbs[j+i+k] = prod->limbs[k];
      }
    }
  }
  */

  //Column based long multiplication. It turns out as long as 
  //MPZ_LENGTH is < 2^32 - 1, (an easy assumption), then 
  //the sum in each column is a 96 bit unsigned int, which means
  //that the carry is 64 bits. 
  uint64_t carry = 0;
  int column;
  for(column = 0; column < MPZ_LENGTH; column++){
    //column_sum will hold 96 bits actually,
    //but any bitwise operation on column_sum will return
    //at most 64.
    uint64_t column_sum = carry;
    int i;
    for(i = 0; i <= column; i++){
      column_sum += a->limbs[column - i] * b->limbs[i];
    }
    //Advance to the next column
    c->limbs[column] = column_sum & 0xFFFFFFFF;
    //The number of bits returned is at most 64, hence why 
    //column_sum is a uint64_t
    //todo unused bits in such a thing really need to return an error
    carry = column_sum >> 32;
  }

  mpz_set(c_,c);
}

/**
  Attempts to multiply size limbs of a and b after some offsets 
  using a single field multiplication
  the first size limbs of a and b are multiplied to form a 2*size limb
  number (unsigned multiplication)
**/
void mpz_mul_infield(mpz_t c, mpz_t a, int a_offset, mpz_t b, int b_offset, int size){
  if (size * 2 > LIMBS_IN_FIELD){
    assert_zero(1); //Would overflow field.
  }
  //We're going to overflow some unsigned ints.
  unsigned int a_, b_, c_;
  int i;
  for(i = size - 1; i >= 0; i--){
    a_ *= 0x100000000ULL;
    b_ *= 0x100000000ULL;
    a_ += a->limbs[i + a_offset];
    b_ += b->limbs[i + b_offset];
  }
  c_ = a_ * b_;
  for(i = 0; i < size*2 && i < MPZ_LENGTH; i++){ //size * 2?
    c->limbs[i] = (c_ >> (32 * i)) & 0xFFFFFFFF;
  }
}

#endif //GMP_H
